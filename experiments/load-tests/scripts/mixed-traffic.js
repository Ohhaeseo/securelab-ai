// SEC-04 + I-8: 정상 사용자와 공격자 트래픽을 섞어 요청 제한의 차단률·오탐률·지연을 잰다.
// 실행: k6 run -e BASE_URL=http://127.0.0.1:18091 -e SUMMARY_JSON=out.json experiments/load-tests/scripts/mixed-traffic.js
// 대상은 로컬 SecureLab backend뿐이다 (실행 스크립트가 대상 확인 후 실행).
import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const BASE = __ENV.BASE_URL || 'http://127.0.0.1:18091';
const DURATION = __ENV.DURATION || '90s';
const PASSWORD = 'correct-horse-battery';

// 정상 사용자: 10명이 합쳐 초당 5건 (1인당 분당 약 30건 — 한도 120의 1/4)
// 공격자: 2명이 합쳐 초당 10건 (1인당 분당 약 300건 — 한도의 2.5배)
export const options = {
  scenarios: {
    normal: {
      executor: 'constant-arrival-rate', exec: 'normalUser', rate: 5, timeUnit: '1s',
      duration: DURATION, preAllocatedVUs: 10, maxVUs: 10,
    },
    attacker: {
      executor: 'constant-arrival-rate', exec: 'attacker', rate: 10, timeUnit: '1s',
      duration: DURATION, preAllocatedVUs: 2, maxVUs: 2,
    },
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
  // k6는 기본으로 반복(iteration)마다 VU의 쿠키를 비운다. 로그인 세션을 유지하려면 꺼야 한다.
  // (이 옵션 없이 잰 첫 측정은 정상 사용자가 익명으로 요청해 무효 — results에서 삭제함)
  noCookiesReset: true,
};

const normalLatency = new Trend('normal_latency', true);
const normalBlocked = new Rate('normal_blocked');
const normalErrors = new Rate('normal_errors');
const attackLatency = new Trend('attack_latency', true);
const attackBlocked = new Rate('attack_blocked');
const attackAllowed = new Rate('attack_allowed');

// VU마다 따로 유지되는 상태 (k6는 VU마다 쿠키 저장소가 따로 있다)
let session = null;

function csrfHeader() {
  const cookies = http.cookieJar().cookiesForURL(BASE);
  const token = (cookies['XSRF-TOKEN'] || [])[0];
  return { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token };
}

function ensureSession(role) {
  if (session) {
    return session;
  }
  http.get(`${BASE}/api/v1/auth/csrf`, { tags: { phase: 'setup' } });
  const email = `k6-${role}-${exec.vu.idInTest}-${Date.now()}@securelab.test`;
  const body = JSON.stringify({ email, password: PASSWORD });
  const signup = http.post(`${BASE}/api/v1/auth/signup`, body, { headers: csrfHeader(), tags: { phase: 'setup' } });
  const login = http.post(`${BASE}/api/v1/auth/login`, body, { headers: csrfHeader(), tags: { phase: 'setup' } });
  check(login, { 'login ok': (r) => r.status === 200 }) || console.error(`login failed ${signup.status}/${login.status}`);
  const doc = http.post(`${BASE}/api/v1/documents`, JSON.stringify({ title: `${role} doc`, content: 'k6 load test' }),
    { headers: csrfHeader(), tags: { phase: 'setup' } });
  session = { documentId: doc.status === 201 ? doc.json('id') : null };
  return session;
}

let debugLogged = 0;

function debugFailure(kind, res) {
  if (__ENV.DEBUG && res.status !== 200 && debugLogged < 3) {
    debugLogged++;
    console.warn(`${kind} vu=${exec.vu.idInTest} ${res.request.method} ${res.url} -> ${res.status} ${String(res.body).substring(0, 120)}`);
  }
}

export function normalUser() {
  const s = ensureSession('normal');
  const url = (exec.scenario.iterationInTest % 2 === 0 && s.documentId)
    ? `${BASE}/api/v1/documents/${s.documentId}` : `${BASE}/api/v1/documents?size=10`;
  const res = http.get(url, { tags: { phase: 'steady', traffic: 'normal' } });
  debugFailure('normal', res);
  normalLatency.add(res.timings.duration);
  normalBlocked.add(res.status === 429);
  normalErrors.add(res.status !== 200 && res.status !== 429);
}

export function attacker() {
  ensureSession('attacker');
  const res = http.get(`${BASE}/api/v1/users/me`, { tags: { phase: 'steady', traffic: 'attack' } });
  attackLatency.add(res.timings.duration);
  attackBlocked.add(res.status === 429);
  attackAllowed.add(res.status === 200);
}

export function handleSummary(data) {
  const out = {};
  out[__ENV.SUMMARY_JSON || 'summary.json'] = JSON.stringify(data, null, 2);
  out.stdout = `normal p50=${data.metrics.normal_latency.values['p(50)'].toFixed(1)}ms `
    + `p95=${data.metrics.normal_latency.values['p(95)'].toFixed(1)}ms `
    + `blocked=${(data.metrics.normal_blocked.values.rate * 100).toFixed(2)}% | `
    + `attack blocked=${(data.metrics.attack_blocked.values.rate * 100).toFixed(1)}%\n`;
  return out;
}
