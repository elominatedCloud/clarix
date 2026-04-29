// Supabase 연결 정보 — 프론트엔드는 anon key만 사용 (RLS가 권한 보호).
// 실제 값은 .env에서 읽지 않고, 빌드 단계 없이 직접 입력합니다 (수업 데모 단순성 우선).
// 배포 전 anon key는 공개되어도 안전하지만, service-role key는 절대 여기에 두지 마세요.

window.CLARIX_CONFIG = {
  supabaseUrl: 'https://ffvxpuhjkvbtkfaektef.supabase.co',
  supabaseAnonKey: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZmdnhwdWhqa3ZidGtmYWVrdGVmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzc0Mjk0OTUsImV4cCI6MjA5MzAwNTQ5NX0.6mMXXg5lDEDK-WcRoGbHfe9XUq-bFAmv-55ZkT0TDUY',
  patientApiUrl: 'http://localhost:8081',
  doctorApiUrl: 'http://localhost:8000',
};
