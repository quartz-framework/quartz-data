package xyz.quartzframework.data.storage;

import xyz.quartzframework.data.page.Page;
import xyz.quartzframework.data.page.Pagination;
import xyz.quartzframework.data.query.Example;

public interface PageableStorage<E> {

    Page<E> find(Example<E> example, Pagination pagination);

    Page<E> findAll(Pagination pagination);

}