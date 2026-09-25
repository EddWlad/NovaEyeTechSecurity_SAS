import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { dedupeGetInterceptor } from './dedupe-get.interceptor';

describe('dedupeGetInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([dedupeGetInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('comparte un GET identico que esta en vuelo', () => {
    const received: unknown[] = [];
    http.get('/api/products/1').subscribe((body) => received.push(body));
    http.get('/api/products/1').subscribe((body) => received.push(body));

    backend.expectOne('/api/products/1').flush({ id: '1' });

    expect(received).toEqual([{ id: '1' }, { id: '1' }]);
  });

  it('no guarda la respuesta: el siguiente GET vuelve a la red', () => {
    const received: unknown[] = [];
    http.get('/api/products/1').subscribe((body) => received.push(body));
    backend.expectOne('/api/products/1').flush({ v: 1 });

    http.get('/api/products/1').subscribe((body) => received.push(body));
    backend.expectOne('/api/products/1').flush({ v: 2 });

    expect(received).toEqual([{ v: 1 }, { v: 2 }]);
  });

  it('distingue los GET por parametros', () => {
    http.get('/api/products', { params: { search: 'domo' } }).subscribe();
    http.get('/api/products', { params: { search: 'bala' } }).subscribe();

    const requests = backend.match((req) => req.url === '/api/products');
    expect(requests.map((req) => req.request.urlWithParams)).toEqual([
      '/api/products?search=domo',
      '/api/products?search=bala',
    ]);
    requests.forEach((req) => req.flush([]));
  });

  it('nunca une peticiones que modifican datos', () => {
    http.post('/api/clients', { name: 'A' }).subscribe();
    http.post('/api/clients', { name: 'A' }).subscribe();

    expect(backend.match('/api/clients').length).toBe(2);
  });

  it('un error llega a todos los que pidieron y libera la clave', () => {
    let errors = 0;
    http.get('/api/products/9').subscribe({ error: () => errors++ });
    http.get('/api/products/9').subscribe({ error: () => errors++ });

    backend.expectOne('/api/products/9').flush(null, { status: 500, statusText: 'Error' });
    expect(errors).toBe(2);

    http.get('/api/products/9').subscribe();
    backend.expectOne('/api/products/9').flush({ id: '9' });
  });
});
