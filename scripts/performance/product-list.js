import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const vus = Number(__ENV.VUS || 20);
const duration = __ENV.DURATION || '2m';

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}/products?orderBy=latest`, { tags: { endpoint: 'product_list' } });
  check(response, { 'product list succeeds': (r) => r.status === 200 });
  sleep(0.2);
}
