package xyz.quartzframework.data;

import lombok.RequiredArgsConstructor;
import xyz.quartzframework.core.bean.annotation.Preferred;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.context.annotation.Configurer;
import xyz.quartzframework.data.query.CompositeQueryParser;

@Configurer(force = true)
@RequiredArgsConstructor
public class QueryParserConfigurer {

    @Provide
    @Preferred
    CompositeQueryParser queryParser() {
        return new CompositeQueryParser();
    }
}