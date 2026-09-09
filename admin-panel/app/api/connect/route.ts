import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || '';
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY || '';

export async function POST(req: NextRequest) {
  try {
    let userKey = '';
    let packageName = '';
    let appName = '';
    let deviceId = '';

    const contentType = req.headers.get('content-type') || '';
    if (contentType.includes('application/x-www-form-urlencoded')) {
      const text = await req.text();
      const params = new URLSearchParams(text);
      userKey = (params.get('user_key') || '').trim();
      packageName = (params.get('package_name') || '').trim();
      appName = (params.get('app_name') || '').trim();
      deviceId = (params.get('device_id') || '').trim();
    } else {
      const json = await req.json().catch(() => ({}));
      userKey = (json.user_key || '').trim();
      packageName = (json.package_name || '').trim();
      appName = (json.app_name || '').trim();
      deviceId = (json.device_id || '').trim();
    }

    if (!userKey) {
      return NextResponse.json({
        status: 'failed',
        reason: 'SDK Key cannot be empty',
        server_mode: 'online',
      });
    }

    if (!supabaseUrl || !supabaseServiceKey) {
      return NextResponse.json({
        status: 'failed',
        reason: 'Authentication service unconfigured',
        server_mode: 'online',
      });
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // 1. Check global maintenance mode
    const { data: config } = await supabase
      .from('system_config')
      .select('maintenance_mode, maintenance_message')
      .eq('id', 'global')
      .maybeSingle();

    if (config && config.maintenance_mode) {
      return NextResponse.json({
        status: 'failed',
        server_mode: 'maintenance',
        message: config.maintenance_message || 'Server under maintenance',
      });
    }

    // 2. Query key from license_keys table
    const { data: keyData, error } = await supabase
      .from('license_keys')
      .select('*')
      .eq('key', userKey)
      .maybeSingle();

    if (error || !keyData) {
      return NextResponse.json({
        status: 'failed',
        reason: 'Invalid SDK Key',
        server_mode: 'online',
      });
    }

    // 3. ENFORCE BCORE SEPARATION: Loader keys CANNOT be used as Bcore SDK keys!
    const isBcoreKey = (keyData.notes && keyData.notes.includes('BCORE_SDK')) ||
                       userKey.startsWith('BCORE') ||
                       userKey.startsWith('SDK-');

    if (!isBcoreKey) {
      return NextResponse.json({
        status: 'failed',
        reason: 'This key is for Loader access only. Valid Bcore SDK key required.',
        server_mode: 'online',
      });
    }

    // 4. Check status
    if (keyData.status === 'BANNED') {
      return NextResponse.json({
        status: 'failed',
        reason: 'This SDK Key has been banned/revoked',
        server_mode: 'online',
      });
    }

    if (keyData.status === 'EXPIRED') {
      return NextResponse.json({
        status: 'failed',
        reason: 'This SDK Key has expired',
        server_mode: 'online',
      });
    }

    const now = new Date();

    // 5. Check expiry if already active
    if (keyData.expires_at) {
      const expDate = new Date(keyData.expires_at);
      if (now > expDate) {
        await supabase
          .from('license_keys')
          .update({ status: 'EXPIRED' })
          .eq('id', keyData.id);

        return NextResponse.json({
          status: 'failed',
          reason: 'This SDK Key has expired',
          server_mode: 'online',
        });
      }
    }

    // 6. Device binding & activation
    let hwidList: string[] = Array.isArray(keyData.hwid_list) ? keyData.hwid_list : [];
    let updatedExpiresAt = keyData.expires_at;
    let updatedStatus = keyData.status;

    if (keyData.status === 'UNUSED') {
      // First activation
      updatedStatus = 'ACTIVE';
      if (keyData.duration_seconds > 0) {
        const exp = new Date(now.getTime() + keyData.duration_seconds * 1000);
        updatedExpiresAt = exp.toISOString();
      }
      if (deviceId && !hwidList.includes(deviceId)) {
        hwidList.push(deviceId);
      }
    } else {
      // Already active, check device
      if (deviceId && !hwidList.includes(deviceId)) {
        if (hwidList.length >= (keyData.max_devices || 1)) {
          return NextResponse.json({
            status: 'failed',
            reason: `Device limit reached (${hwidList.length}/${keyData.max_devices || 1})`,
            server_mode: 'online',
          });
        }
        hwidList.push(deviceId);
      }
      if (!updatedExpiresAt && keyData.duration_seconds > 0) {
        const exp = new Date(now.getTime() + keyData.duration_seconds * 1000);
        updatedExpiresAt = exp.toISOString();
      }
    }

    const clientIp =
      req.headers.get('x-forwarded-for')?.split(',')[0].trim() ||
      req.headers.get('x-real-ip') ||
      '';

    await supabase
      .from('license_keys')
      .update({
        status: updatedStatus,
        hwid_list: hwidList,
        activated_at: keyData.activated_at || now.toISOString(),
        expires_at: updatedExpiresAt,
        last_login_at: now.toISOString(),
        last_ip: clientIp,
      })
      .eq('id', keyData.id);

    // Format expiry for Bcore SimpleDateFormat ("yyyy-MM-dd HH:mm:ss")
    let formattedExpiry = '';
    if (updatedExpiresAt) {
      const d = new Date(updatedExpiresAt);
      const pad = (n: number) => String(n).padStart(2, '0');
      formattedExpiry = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    }

    return NextResponse.json({
      status: 'success',
      server_mode: 'online',
      message: 'SDK Activated Successfully',
      expiry: formattedExpiry,
      toggle_expiry: formattedExpiry ? 1 : 0,
      feature1: 1, // Enable Daemon service
      feature2: 1, // Enable Root Hide
    });
  } catch (err: any) {
    return NextResponse.json({
      status: 'failed',
      reason: err.message || 'Internal server error',
      server_mode: 'online',
    });
  }
}
