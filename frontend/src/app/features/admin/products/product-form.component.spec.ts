import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductFormComponent } from './product-form.component';

describe('ProductFormComponent', () => {
  let fixture: ComponentFixture<ProductFormComponent>;
  let component: ProductFormComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductFormComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductFormComponent);
    component = fixture.componentInstance;
    component.categories = [{ id: 1, name: 'WEB', slug: 'web', description: null, active: true }];
    fixture.detectChanges();
  });

  it('marks the form invalid when required fields are missing', () => {
    component.form.reset({
      sku: '',
      name: '',
      slug: '',
      description: '',
      categoryId: 0,
      price: -1
    });

    expect(component.form.invalid).toBe(true);
    expect(component.form.controls['price'].hasError('min')).toBe(true);
    expect(component.form.controls['categoryId'].hasError('min')).toBe(true);
  });

  it('emits a payload when the form is valid', () => {
    const emittedPayloads: unknown[] = [];
    component.saveProduct.subscribe((payload) => emittedPayloads.push(payload));

    component.form.setValue({
      sku: 'LF-NEW-001',
      name: 'New Product',
      slug: 'new-product',
      description: 'Valid form',
      categoryId: 1,
      price: 1500
    });

    component.submit();

    expect(emittedPayloads).toEqual([
      {
        sku: 'LF-NEW-001',
        name: 'New Product',
        slug: 'new-product',
        description: 'Valid form',
        categoryId: 1,
        price: 1500
      }
    ]);
  });
});
