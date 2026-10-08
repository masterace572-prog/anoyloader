package top.niunaijun.blackbox.core.system;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.IBActivityThread;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.notification.BNotificationManagerService;
import top.niunaijun.blackbox.core.system.pm.BPackageManagerService;
import top.niunaijun.blackbox.core.system.user.BUserHandle;
import top.niunaijun.blackbox.entity.AppConfig;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.PermissionUtils;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.ApplicationThreadCompat;
import top.niunaijun.blackbox.utils.compat.BuildCompat;
import top.niunaijun.blackbox.utils.compat.BundleCompat;
import top.niunaijun.blackbox.utils.provider.ProviderCall;
import top.niunaijun.blackbox.core.system.api.MetaActivationManager;

/**
 * Created by @RIYAZXERO on 4/2/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class BProcessManagerService implements ISystemService {
    public static final String TAG = "BProcessManager";

    public static BProcessManagerService sBProcessManagerService = new BProcessManagerService();
    private final Map<Integer, Map<String, ProcessRecord>> mProcessMap = new HashMap<>();
    private final List<ProcessRecord> mPidsSelfLocked = new ArrayList<>();
    private final Object mProcessLock = new Object();

    public static BProcessManagerService get() {
        return sBProcessManagerService;
    }

    public ProcessRecord startProcessLocked(String packageName, String processName, int userId, int bpid, int callingPid) {
        ApplicationInfo info = BPackageManagerService.get().getApplicationInfo(packageName, 0, userId);
        if (info == null)
            return null;
        ProcessRecord app;
        int buid = BUserHandle.getUid(userId, BPackageManagerService.get().getAppId(packageName));
        synchronized (mProcessLock) {
            Map<String, ProcessRecord> bProcess = mProcessMap.get(buid);

            if (bProcess == null) {
                bProcess = new HashMap<>();
            }
            if (bpid == -1) {
                app = bProcess.get(processName);
                if (app != null) {
                    if (app.initLock != null) {
                        // Initialization is synchronous under mProcessLock. A closed
                        // gate means reentry/incomplete state; never wait indefinitely
                        // while holding the lock needed by death/cleanup callbacks.
                        if (!app.initLock.block(1)) return null;
                    }
                    if (app.bActivityThread != null && app.bActivityThread.asBinder().isBinderAlive()) {
                        return app;
                    }
                    // Do not hand out a dead Binder or retain a stale slot reservation.
                    bProcess.remove(processName);
                    mPidsSelfLocked.remove(app);
                }
                bpid = getUsingBPidL();
                Slog.d(TAG, "init bUid = " + buid + ", bPid = " + bpid);
            }
            if (bpid == -1) {
                throw new RuntimeException("No processes available");
            }
            app = new ProcessRecord(info, processName);
            app.uid = Process.myUid();
            app.bpid = bpid;
            app.buid = buid;
            app.callingBUid = getBUidByPidOrPackageName(callingPid, packageName);
            app.userId = userId;

            bProcess.put(processName, app);
            mPidsSelfLocked.add(app);

            mProcessMap.put(buid, bProcess);
            boolean initialized = false;
            try {
                initialized = initAppProcessL(app);
                if (initialized) {
                    app.pid = getPid(BlackBoxCore.getContext(), ProxyManifest.getProcessName(app.bpid));
                }
            } finally {
                app.initLock.open();
                if (!initialized) {
                    // Also executes if provider/attach throws. Do not leave a
                    // registered half-process or strand initialization waiters.
                    if (bProcess.get(processName) == app) bProcess.remove(processName);
                    mPidsSelfLocked.remove(app);
                    if (bProcess.isEmpty()) mProcessMap.remove(buid);
                    removeProc(app);
                }
            }
            if (!initialized) app = null;
        }
        return app;
    }

    // 20240801 add request permission add start 0
    private void requestPermissionIfNeed(ProcessRecord app) {
        if (PermissionUtils.isCheckPermissionRequired(app.info)) {
            String[] permissions = BPackageManagerService.get().getDangerousPermissions(app.info.packageName);
            new Thread(() -> {
				if (!PermissionUtils.checkPermissions(permissions)) {
					ConditionVariable permissionLock = new ConditionVariable();
					startRequestPermission(permissions, permissionLock);
					permissionLock.block();
				}
			}).start();
        }
    }

    private void startRequestPermission(String[] permissions, final ConditionVariable permissionLock) {
	   if (permissions == null || permissions.length == 0) {
		   if (permissionLock != null) {
			   permissionLock.open();
		   }
		   return;
	   }
	   if (BlackBoxCore.getContext() == null || permissionLock == null) {
		   return;
	   }
	   PermissionUtils.startRequestPermissions(BlackBoxCore.getContext(), permissions, new PermissionUtils.CallBack() {
	   @Override
	   public boolean onResult(int requestCode, String[] permissions, int[] grantResults) {
		 try {
		     return PermissionUtils.isRequestGranted(grantResults);
			 } finally {
			 permissionLock.open();
		     }
		  }
	   });
	}

    // 20240801 add request permission add end 0

    private int getUsingBPidL() {
        ActivityManager manager = (ActivityManager) BlackBoxCore.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses =
                manager != null ? manager.getRunningAppProcesses() : null;
        Set<Integer> usingPs = new HashSet<>();
        // Android's snapshot may omit a process still being initialized. Reserve
        // tracked slots too, otherwise a child process can displace a live game.
        for (ProcessRecord record : mPidsSelfLocked) usingPs.add(record.bpid);
        if (runningAppProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
                usingPs.add(parseBPid(runningAppProcess.processName));
            }
        }
        for (int i = 0; i < ProxyManifest.FREE_COUNT; i++) {
            if (usingPs.contains(i)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    public void restartAppProcess(String packageName, String processName, int userId) {
        synchronized (mProcessLock) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            ProcessRecord app;
            synchronized (mProcessLock) {
                app = findProcessByPid(callingPid);
            }
            if (app == null) {
                String stubProcessName = getProcessName(BlackBoxCore.getContext(), callingPid);
                int bpid = parseBPid(stubProcessName);
                startProcessLocked(packageName, processName, userId, bpid, callingPid);
            }
        }
    }

    private int parseBPid(String stubProcessName) {
        String prefix;
        if (stubProcessName == null) {
            return -1;
        } else {
            prefix = BlackBoxCore.getHostPkg() + ":p";
        }
        if (stubProcessName.startsWith(prefix)) {
            try {
                return Integer.parseInt(stubProcessName.substring(prefix.length()));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return -1;
    }


    //这里初始化了userinfo
    private boolean initAppProcessL(ProcessRecord record) {
		Log.d(TAG, "initProcess: " + record.processName);
		requestPermissionIfNeed(record);
		AppConfig appConfig = record.getClientConfig();
		Bundle bundle = new Bundle();
		bundle.putParcelable(AppConfig.KEY, appConfig);
		// 🔥 CRASH FIX: Line 209
		Bundle result;
		try {
			result = ProviderCall.callSafely(record.getProviderAuthority(), "_Black_|_init_process_", (String) null, bundle);
		} catch (Exception e) {
			Log.e(TAG, "Provider error: " + e.getMessage());
			result = new Bundle();
		}
		if (result == null) return false;
        IBinder appThread = BundleCompat.getBinder(result, "_Black_|_client_");
		if (appThread == null || !appThread.isBinderAlive()) {
			return false;
		}
        if (!attachClientL(record, appThread)) return false;
		createProc(record);
		return true;
	}

    private boolean attachClientL(final ProcessRecord app, final IBinder appThread) {
        IBActivityThread activityThread = IBActivityThread.Stub.asInterface(appThread);
        if (activityThread == null) return false;
        final IBinder.DeathRecipient recipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                Log.d(TAG, "App Died: " + app.processName);
                onProcessDie(app);
            }
        };
        boolean linked = false;
        boolean attached = false;
        try {
            // A failed link means the process is already dead: do not publish it.
            appThread.linkToDeath(recipient, 0);
            linked = true;
            android.os.IInterface client = ApplicationThreadCompat.asInterface(activityThread.getActivityThread());
            if (client == null || !appThread.isBinderAlive()) return false;
            app.appThread = client;
            app.bActivityThread = activityThread;
            attached = true;
            return true;
        } catch (RemoteException e) {
            Log.w(TAG, "Virtual client died during attach", e);
            return false;
        } finally {
            if (linked && !attached) {
                try { appThread.unlinkToDeath(recipient, 0); } catch (RuntimeException ignored) { }
            }
            app.initLock.open();
        }
    }

    public void onProcessDie(ProcessRecord record) {
        synchronized (mProcessLock) {
            // Binder death can arrive after a replacement was registered. Never
            // kill by an old PID (it may have been reused) or remove that replacement.
            Map<String, ProcessRecord> process = mProcessMap.get(record.buid);
            boolean current = process != null && process.get(record.processName) == record;
            if (current) {
                process.remove(record.processName);
                if (process.isEmpty()) {
                    mProcessMap.remove(record.buid);
                }
            }
            mPidsSelfLocked.remove(record);
            boolean slotReused = false;
            for (ProcessRecord live : mPidsSelfLocked) {
                if (live.bpid == record.bpid) { slotReused = true; break; }
            }
            if (!slotReused) removeProc(record);
            if (current) {
                BNotificationManagerService.get().deletePackageNotification(record.getPackageName(), record.userId);
            }
        }
    }

    public ProcessRecord findProcessRecord(String packageName, String processName, int userId) {
        synchronized (mProcessLock) {
            int appId = BPackageManagerService.get().getAppId(packageName);
            int buid = BUserHandle.getUid(userId, appId);
            Map<String, ProcessRecord> processRecordMap = mProcessMap.get(buid);
            if (processRecordMap == null)
                return null;
            return processRecordMap.get(processName);
        }
    }

    public void killAllByPackageName(String packageName) {
        synchronized (mProcessLock) {
            synchronized (mPidsSelfLocked) {
                List<ProcessRecord> tmp = new ArrayList<>(mPidsSelfLocked);
                int appId = BPackageManagerService.get().getAppId(packageName);
                for (ProcessRecord processRecord : mPidsSelfLocked) {
                    int appId1 = BUserHandle.getAppId(processRecord.buid);
                    if (appId == appId1) {
                        mProcessMap.remove(processRecord.buid);
                        tmp.remove(processRecord);
                        processRecord.kill();
                    }
                }
                mPidsSelfLocked.clear();
                mPidsSelfLocked.addAll(tmp);
            }
        }
    }

    public void killPackageAsUser(String packageName, int userId) {
        synchronized (mProcessLock) {
            int buid = BUserHandle.getUid(userId, BPackageManagerService.get().getAppId(packageName));
            Map<String, ProcessRecord> process = mProcessMap.get(buid);
            if (process == null)
                return;
            for (ProcessRecord value : process.values()) {
                value.kill();
                mPidsSelfLocked.remove(value);
            }
            mProcessMap.remove(buid);
        }
    }

    public List<ProcessRecord> getPackageProcessAsUser(String packageName, int userId) {
        synchronized (mProcessLock) {
            int buid = BUserHandle.getUid(userId, BPackageManagerService.get().getAppId(packageName));
            Map<String, ProcessRecord> process = mProcessMap.get(buid);
            if (process == null)
                return new ArrayList<>();
            return new ArrayList<>(process.values());
        }
    }

    public int getBUidByPidOrPackageName(int pid, String packageName) {
        synchronized (mProcessLock) {
            ProcessRecord callingProcess = BProcessManagerService.get().findProcessByPid(pid);
            if (callingProcess == null) {
                return BPackageManagerService.get().getAppId(packageName);
            }
            return BUserHandle.getAppId(callingProcess.buid);
        }
    }

    public int getUserIdByCallingPid(int callingPid) {
        synchronized (mProcessLock) {
            ProcessRecord callingProcess = BProcessManagerService.get().findProcessByPid(callingPid);
            if (callingProcess == null) {
                return 0;
            }
            return callingProcess.userId;
        }
    }

    public ProcessRecord findProcessByPid(int pid) {
        synchronized (mPidsSelfLocked) {
            for (ProcessRecord processRecord : mPidsSelfLocked) {
                if (processRecord.pid == pid)
                    return processRecord;
            }
            return null;
        }
    }

    private static String getProcessName(Context context, int pid) {
        String processName = null;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.pid == pid) {
                processName = info.processName;
                break;
            }
        }
        if (processName == null) {
            throw new RuntimeException("processName = null");
        }
        return processName;
    }

    public static int getPid(Context context, String processName) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
                if (runningAppProcess.processName.equals(processName)) {
                    return runningAppProcess.pid;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void createProc(ProcessRecord record) {
        File cmdline = new File(BEnvironment.getProcDir(record.bpid), "cmdline");
        try {
            FileUtils.writeToFile(record.processName.getBytes(), cmdline);
        } catch (IOException ignored) {
        }
    }

    private static void removeProc(ProcessRecord record) {
        FileUtils.deleteDir(BEnvironment.getProcDir(record.bpid));
    }

    @Override
    public void systemReady() {
        FileUtils.deleteDir(BEnvironment.getProcDir());
    }

}
