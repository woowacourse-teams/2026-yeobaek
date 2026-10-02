package yeobaek.backend.appreciation.api;

import java.util.List;

public interface AppreciationSubtypeEraserRegistry {

    AppreciationSubtypeEraser get(String kind);

    List<AppreciationSubtypeEraser> all();
}
