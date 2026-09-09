package com.ryzen.utils;

/**
 * Configuration file for Supabase backend connection.
 */
public class SupabaseConfig {

    // Supabase Live Project URL
    public static final String SUPABASE_URL = "https://wawjxbwahndpjtmonobs.supabase.co";

    // Supabase public anon key (safe for client-side mobile applications)
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Indhd2p4YndhaG5kcGp0bW9ub2JzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg0MzUyOTYsImV4cCI6MjEwNDAxMTI5Nn0.e0l9tMfRqaiuJAtt6mU09-dp6UMhOeFPZzku1VHMKAw";

    // Live Vercel API endpoint
    public static final String VERCEL_LOGIN_API_URL = "https://anoyloader.vercel.app/api/client/login";

    /**
     * Checks whether Supabase is configured with custom credentials.
     */
    public static boolean isConfigured() {
        return SUPABASE_URL != null && !SUPABASE_URL.isEmpty() && !SUPABASE_URL.contains("your-project-id");
    }
}
