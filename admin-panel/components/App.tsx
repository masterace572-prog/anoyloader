"use client";
import React, { useEffect, useState } from "react";
import Link from "next/link";
import { api, clearAdminToken } from "@/lib/api";
import { supabase } from "@/lib/supabase";
import { FeedbackProvider } from "./feedback";
import { LicensesView } from "./LicensesView";
import { LoginView } from "./LoginView";
import { AppView, Shell } from "./Shell";
import { SystemView } from "./SystemView";
import { UpdatesView } from "./UpdatesView";
import { ApiKeysView } from "./ApiKeysView";
import { Spinner, Button } from "./ui";
function Dashboard() {
  const [view, setView] = useState<AppView>("licenses");
  const [live, setLive] = useState(false);
  return (
    <Shell
      view={view}
      onView={setView}
      live={live}
      onLogout={async () => {
        await clearAdminToken();
        window.location.assign("/login");
      }}
    >
      {view === "licenses" && <LicensesView onLiveChange={setLive} />}
      {view === "updates" && <UpdatesView />}
      {view === "system" && <SystemView />}
      {view === "api" && <ApiKeysView />}
    </Shell>
  );
}
export default function App() {
  const [checking, setChecking] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [message, setMessage] = useState("");
  const [configured, setConfigured] = useState(true);
  async function check() {
    setChecking(true);
    try {
      const data = await api<{
        authenticated: boolean;
        configured: boolean;
        error?: string;
        status?: number;
      }>("/api/admin/auth");
      setAuthenticated(data.authenticated);
      setConfigured(data.configured);
      setMessage(
        data.status === 403 || data.status === 503
          ? data.error || "Access unavailable."
          : "",
      );
    } catch {
      setMessage("Unable to verify access. Please retry.");
    } finally {
      setChecking(false);
    }
  }
  useEffect(() => {
    void check();
    const subscription = supabase?.auth.onAuthStateChange(() => {
      setTimeout(() => void check(), 0);
    });
    return () => subscription?.data.subscription.unsubscribe();
  }, []);
  if (checking)
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner />
      </div>
    );
  if (!authenticated && message)
    return (
      <div className="mx-auto mt-24 max-w-md space-y-5 rounded-xl border border-line p-8">
        <h1 className="text-xl">Account access</h1>
        <p role="status" className="text-muted">
          {message}
        </p>
        <Button onClick={check}>Check again</Button>
        <Button
          variant="secondary"
          onClick={async () => {
            await clearAdminToken();
            window.location.assign("/login");
          }}
        >
          Sign out
        </Button>
        <Link href="/" className="block text-sm underline">
          Back home
        </Link>
      </div>
    );
  if (!authenticated)
    return <LoginView configured={configured} onAuthenticated={check} />;
  return (
    <FeedbackProvider>
      <Dashboard />
    </FeedbackProvider>
  );
}
