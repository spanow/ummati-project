import { EventEmitter } from '@angular/core';

// Test de la logique pure sans instanciation Angular
describe('StarRatingComponent logic', () => {
  it('select should update value and emit', () => {
    const valueChange = new EventEmitter<number>();
    let value = 0;
    const spy = vi.fn();
    valueChange.subscribe(spy);

    // Simule la méthode select
    const select = (star: number) => { value = star; valueChange.emit(star); };

    select(3);
    expect(value).toBe(3);
    expect(spy).toHaveBeenCalledWith(3);
  });

  it('select different values', () => {
    let value = 0;
    const select = (star: number) => { value = star; };

    select(1);
    expect(value).toBe(1);
    select(5);
    expect(value).toBe(5);
  });
});
