import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const vus = Number(__ENV.VUS || 20);
const duration = __ENV.DURATION || '2m';
const sellerUsername = __ENV.PERF_SELLER || 'perf-seller';
const buyerUsername = __ENV.PERF_BUYER || 'perf-buyer';
const password = __ENV.PERF_PASSWORD || 'PerfPass_2026!';

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  },
};

const json = { headers: { 'Content-Type': 'application/json' } };
function request(method, path, body, token, name) {
  const params = { ...json, tags: { endpoint: name } };
  if (token) params.headers = { ...params.headers, Authorization: `Bearer ${token}` };
  return http.request(method, `${baseUrl}${path}`, body ? JSON.stringify(body) : null, params);
}
function login(username) {
  const login = request('POST', '/users/authenticate', { username, password }, null, 'login');
  const result = login.json();
  return result?.data?.token;
}

export function setup() {
  const seller = login(sellerUsername);
  const buyer = login(buyerUsername);
  if (!seller || !buyer) throw new Error('performance fixture login failed');
  return { seller, buyer };
}

export default function (tokens) {
  const id = `${__VU}-${__ITER}-${Date.now()}`;
  const product = request('POST', '/products', {
    title: `perf-product-${id}`, description: 'performance test item', price: 10,
    categoryId: 1, conditionLevel: 5, coverImage: 'https://example.com/perf.png', deliveryOptions: ['MEETUP'],
  }, tokens.seller, 'create_product');
  check(product, { 'product created': (r) => r.status === 200 });
  const productId = product.json()?.data?.id;
  if (!productId) return;

  const order = request('POST', '/orders', { productId: String(productId), deliveryMethod: 'MEETUP', meetupLocationId: 1 }, tokens.buyer, 'create_order');
  check(order, { 'order created': (r) => r.status === 200 });
  const orderId = order.json()?.data?.id;
  if (!orderId) return;

  const payment = request('POST', `/orders/${orderId}/pay`, { requestId: `perf-pay-${id}` }, tokens.buyer, 'payment_freeze');
  check(payment, { 'payment frozen': (r) => r.status === 200 });
  const completed = request('POST', `/orders/${orderId}/confirm-completion`, null, tokens.buyer, 'confirm_completion');
  check(completed, { 'order completed': (r) => r.status === 200 });
  sleep(0.1);
}
