package com.yalcinkaya.ctf.util.parametrization;

import java.util.Iterator;

public interface Domain<E> {
    boolean contains(E element);

    Iterator<E> iterator(int sampleSize);
}
