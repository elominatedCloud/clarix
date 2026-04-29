import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const cfg = window.CLARIX_CONFIG;
if (!cfg?.supabaseUrl || cfg.supabaseAnonKey.includes('REPLACE_ME')) {
  console.warn('[Clarix] config.js의 Supabase 값을 채워주세요.');
}

export const supabase = createClient(cfg.supabaseUrl, cfg.supabaseAnonKey);

export async function getAccessToken() {
  const { data } = await supabase.auth.getSession();
  return data.session?.access_token ?? null;
}

export async function authedFetch(url, init = {}) {
  const token = await getAccessToken();
  return fetch(url, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers ?? {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
}
