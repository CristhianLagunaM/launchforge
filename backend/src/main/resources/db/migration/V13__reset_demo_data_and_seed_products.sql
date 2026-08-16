TRUNCATE TABLE
    audit_log,
    order_discounts,
    order_items,
    orders,
    inventory,
    discount_configuration,
    user_roles,
    users,
    products,
    categories
RESTART IDENTITY CASCADE;

INSERT INTO categories (
    id,
    name,
    slug,
    description,
    active,
    created_at,
    updated_at
)
VALUES
    (
        1,
        'WEB',
        'web',
        'Soluciones para presencia web, páginas corporativas y experiencias digitales.',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        2,
        'ECOMMERCE',
        'ecommerce',
        'Soluciones de comercio electrónico, catálogos y experiencias de compra.',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        3,
        'SAAS',
        'saas',
        'Productos digitales y plataformas SaaS listas para escalar.',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        4,
        'DESIGN',
        'design',
        'Servicios de diseño de producto, experiencia de usuario e interfaces.',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        5,
        'INTEGRATIONS',
        'integrations',
        'Integraciones con APIs, servicios externos y plataformas empresariales.',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        6,
        'MAINTENANCE',
        'maintenance',
        'Servicios de mantenimiento, soporte y evolución continua de software.',
        TRUE,
        NOW(),
        NOW()
    );

SELECT setval(
    pg_get_serial_sequence('categories', 'id'),
    (SELECT MAX(id) FROM categories)
);

INSERT INTO products (
    id,
    sku,
    name,
    slug,
    description,
    category_id,
    price,
    active,
    created_at,
    updated_at,
    created_by,
    updated_by
)
VALUES
    (
        '9d1f4e16-0dc6-4da8-8604-cc83348fd350',
        'LF-LANDING-001',
        'Landing Page Launch',
        'landing-page-launch',
        'Landing page profesional orientada a conversión, optimizada para dispositivos móviles e integrada con formularios de contacto y analítica.',
        1,
        1800000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        '0f1f155c-2088-40ce-8db1-43f3b02913fd',
        'LF-CORP-001',
        'Corporate Website Pro',
        'corporate-website-pro',
        'Sitio web corporativo completo con administración de contenidos, formularios, secciones institucionales y despliegue preparado para producción.',
        1,
        4500000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        'fa642e46-91e5-4f03-b617-10919691e298',
        'LF-ECOM-001',
        'E-commerce Starter',
        'ecommerce-starter',
        'Tienda virtual con catálogo de productos, carrito de compras, proceso de checkout y estructura preparada para integrar una pasarela de pagos.',
        2,
        6800000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        '4b6e7257-324c-42bf-93f7-a6c6c96fdc54',
        'LF-CAT-001',
        'Digital Catalog',
        'digital-catalog',
        'Catálogo digital pensado para presentar productos o servicios de manera clara, rápida y adaptable a diferentes dispositivos.',
        2,
        2900000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        'c37b91a7-28c8-43d6-978f-9d3cb5038f85',
        'LF-SAAS-001',
        'SaaS MVP Forge',
        'saas-mvp-forge',
        'Desarrollo de un MVP SaaS con autenticación, panel de usuario, arquitectura escalable y bases técnicas para incorporar facturación.',
        3,
        12500000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        '6a99bc84-72a0-4ea0-9f8c-25589e4f1e0a',
        'LF-UX-001',
        'UX/UI Discovery',
        'ux-ui-discovery',
        'Proceso de descubrimiento y diseño que incluye análisis funcional, definición de experiencia, wireframes y propuesta visual de interfaz.',
        4,
        2200000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        'e2f77e1a-4c56-46f1-8707-b097614c1624',
        'LF-API-001',
        'API Integration Pack',
        'api-integration-pack',
        'Implementación de integraciones entre sistemas mediante APIs REST, servicios externos y mecanismos seguros de intercambio de información.',
        5,
        3500000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        '15b55280-7f43-4788-80a7-f89ca34df4fa',
        'LF-MNT-001',
        'Monthly Maintenance',
        'monthly-maintenance',
        'Plan mensual de mantenimiento para corrección de incidencias, pequeñas mejoras, monitoreo técnico y soporte sobre aplicaciones existentes.',
        6,
        1500000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        'b9d220ad-64fa-4eb5-a868-63431fbceeb4',
        'LF-DASH-001',
        'Analytics Dashboard',
        'analytics-dashboard',
        'Dashboard web para visualizar indicadores, métricas de negocio y datos operativos mediante componentes interactivos y reportes consolidados.',
        3,
        7200000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    ),
    (
        '73c51e77-a9ce-49e5-a5ae-6cc1f74bd4aa',
        'LF-AUTO-001',
        'Business Automation',
        'business-automation',
        'Automatización de procesos empresariales mediante integración de servicios, reglas de negocio y flujos que reducen tareas manuales repetitivas.',
        5,
        5900000.00,
        TRUE,
        NOW(),
        NOW(),
        NULL,
        NULL
    );