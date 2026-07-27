import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'LoadTest123';

export const options = {
    vus: 100,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<100'],
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    const username = `loadtest_${Date.now()}`;
    const headers = { 'Content-Type': 'application/json' };

    http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
        username: username,
        email: `${username}@loadtest.local`,
        password: PASSWORD,
    }), { headers });

    const loginResponse = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
        username: username,
        password: PASSWORD,
    }), { headers });

    return { token: JSON.parse(loginResponse.body).token };
}

export default function (data) {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`,
        },
    };

    const response = http.post(`${BASE_URL}/api/reservations`, '{}', params);

    check(response, {
        'reservation resolved without server error': (r) => r.status === 201 || r.status === 409,
    });

    sleep(0.1);
}