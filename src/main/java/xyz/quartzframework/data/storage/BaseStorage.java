package xyz.quartzframework.data.storage;

import xyz.quartzframework.data.query.Example;

import java.util.List;
import java.util.Optional;

public interface BaseStorage<E, ID> {

    Optional<E> findById(ID id);

    long count();

    long count(Example<E> example);

    boolean exists(ID id);

    boolean exists(Example<E> example);

    E save(E entity);

    List<E> save(Iterable<E> entities);

    void deleteById(ID id);

    void delete(E entity);

    void delete(Example<E> example);

    void delete(Iterable<E> entities);

}