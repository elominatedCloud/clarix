import { supabase } from '../lib/supabase.js';

const logoutBtn = document.getElementById('logout');
logoutBtn?.addEventListener('click', async () => {
  await supabase.auth.signOut();
  location.href = '/';
});
