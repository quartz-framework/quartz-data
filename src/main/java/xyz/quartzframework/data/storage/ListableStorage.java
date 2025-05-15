package xyz.quartzframework.data.storage;

import xyz.quartzframework.data.page.Sort;
import xyz.quartzframework.data.query.Example;

import java.util.List;

public interface ListableStorage<E> {

    List<E> find(Example<E> example);

    List<E> find(Example<E> example, Sort sort);

    List<E> findAll();

    List<E> findAll(Sort sort);

}