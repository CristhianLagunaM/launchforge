import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OrdersStore } from './orders.store';

describe('OrdersStore', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
  });

  it('creates an order and keeps the latest created order in state', async () => {
    const store = TestBed.inject(OrdersStore);
    const httpTestingController = TestBed.inject(HttpTestingController);

    const createPromise = store.createOrder(
      {
        items: [
          {
            productId: '22222222-2222-2222-2222-222222222221',
            quantity: 1
          }
        ],
        requirementDescription:
          'Necesito una landing page para validar el flujo de creación.',
        projectObjective:
          'Validar que el store conserve la última orden creada.',
        contactEmail:
          'customer@launchforge.dev',
        contactPhone:
          '+57 300 000 0000',
        desiredDeliveryDate:
          '2026-09-15',
        referencesUrl:
          'https://example.com/order-reference'
      },
      'frontend-idem-001'
    );

    const request =
      httpTestingController.expectOne(
        '/api/v1/orders'
      );

    expect(
      request.request.method
    ).toBe(
      'POST'
    );

    expect(
      request.request.headers.get(
        'Idempotency-Key'
      )
    ).toBe(
      'frontend-idem-001'
    );

    expect(
      request.request.body
    ).toEqual({
      items: [
        {
          productId:
            '22222222-2222-2222-2222-222222222221',
          quantity: 1
        }
      ],
      requirementDescription:
        'Necesito una landing page para validar el flujo de creación.',
      projectObjective:
        'Validar que el store conserve la última orden creada.',
      contactEmail:
        'customer@launchforge.dev',
      contactPhone:
        '+57 300 000 0000',
      desiredDeliveryDate:
        '2026-09-15',
      referencesUrl:
        'https://example.com/order-reference'
    });

    request.flush({
      id:
        '55555555-5555-5555-5555-555555555501',
      orderNumber:
        'LF-2026-ABC12345',
      customerId:
        '11111111-1111-1111-1111-111111111112',
      customerEmail:
        'customer@launchforge.dev',
      status:
        'CREATED',
      idempotencyKey:
        'frontend-idem-001',
      subtotal:
        1200,
      discountTotal:
        0,
      total:
        1200,
      requirementDescription:
        'Necesito una landing page para validar el flujo de creación.',
      projectObjective:
        'Validar que el store conserve la última orden creada.',
      contactEmail:
        'customer@launchforge.dev',
      contactPhone:
        '+57 300 000 0000',
      desiredDeliveryDate:
        '2026-09-15',
      referencesUrl:
        'https://example.com/order-reference',
      createdAt:
        '2026-08-14T12:00:00Z',
      updatedAt:
        '2026-08-14T12:00:00Z',
      items: [
        {
          id:
            '66666666-6666-6666-6666-666666666601',
          productId:
            '22222222-2222-2222-2222-222222222221',
          sku:
            'LF-LANDING-001',
          productName:
            'Landing Page Launch',
          quantity:
            1,
          unitPrice:
            1200,
          subtotal:
            1200
        }
      ],
      discounts: []
    });

    const createdOrder =
      await createPromise;

    expect(
      createdOrder?.id
    ).toBe(
      '55555555-5555-5555-5555-555555555501'
    );

    expect(
      createdOrder?.status
    ).toBe(
      'CREATED'
    );

    expect(
      createdOrder?.requirementDescription
    ).toBe(
      'Necesito una landing page para validar el flujo de creación.'
    );

    expect(
      store.latestCreatedOrder()?.id
    ).toBe(
      '55555555-5555-5555-5555-555555555501'
    );

    expect(
      store.orders()
    ).toHaveLength(
      1
    );

    httpTestingController.verify();
  });

  it('captures problem details when order creation fails', async () => {
    const store = TestBed.inject(OrdersStore);
    const httpTestingController = TestBed.inject(HttpTestingController);

    const createPromise = store.createOrder(
      {
        items: [
          {
            productId: '22222222-2222-2222-2222-222222222221',
            quantity: 1
          }
        ],
        requirementDescription:
          'Necesito validar el comportamiento cuando no hay inventario.',
        projectObjective:
          'Comprobar el manejo del conflicto de capacidad.',
        contactEmail:
          'customer@launchforge.dev',
        contactPhone:
          '+57 300 000 0000'
      },
      'frontend-idem-002'
    );

    const request =
      httpTestingController.expectOne(
        '/api/v1/orders'
      );

    expect(
      request.request.method
    ).toBe(
      'POST'
    );

    expect(
      request.request.headers.get(
        'Idempotency-Key'
      )
    ).toBe(
      'frontend-idem-002'
    );

    request.flush(
      {
        type:
          'https://launchforge/errors/inventory/insufficient-capacity',
        title:
          'Inventory conflict',
        status:
          409,
        detail:
          'Not enough available capacity.',
        productId:
          '22222222-2222-2222-2222-222222222221',
        sku:
          'LF-CORP-001',
        productName:
          'Corporate Website Suite',
        availableQuantity:
          2,
        requestedQuantity:
          5
      },
      {
        status:
          409,
        statusText:
          'Conflict'
      }
    );

    const result =
      await createPromise;

    expect(
      result
    ).toBeNull();

    expect(
      store.error()
    ).toBe(
      'No hay capacidad suficiente para completar uno de los productos. Ajusta la cantidad en el carrito e intenta nuevamente.'
    );

    expect(
      store.capacityConflict()
    ).toEqual({
      productId:
        '22222222-2222-2222-2222-222222222221',
      sku:
        'LF-CORP-001',
      productName:
        'Corporate Website Suite',
      availableQuantity:
        2,
      requestedQuantity:
        5
    });

    store.clearCreationFeedback();

    expect(
      store.error()
    ).toBeNull();

    expect(
      store.capacityConflict()
    ).toBeNull();

    httpTestingController.verify();
  });
});
