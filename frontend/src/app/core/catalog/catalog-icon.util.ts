const CATEGORY_ICONS: Readonly<Record<string, string>> = {
  design: 'palette',
  ecommerce: 'shopping_cart',
  integration: 'link',
  integrations: 'link',
  maintenance: 'build',
  marketing: 'trending_up',
  saas: 'cloud',
  web: 'language'
};

export function catalogCategoryIcon(categorySlug: string): string {
  return CATEGORY_ICONS[categorySlug.trim().toLowerCase()] ?? 'code';
}
