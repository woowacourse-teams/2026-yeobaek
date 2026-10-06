package yeobaek.backend.appreciation.spi.erasure;

import java.util.List;
import yeobaek.backend.appreciation.api.AppreciationKind;

public interface AppreciationSubtypeEraserRegistry {

    AppreciationSubtypeEraser get(AppreciationKind kind);

    List<AppreciationSubtypeEraser> all();
}
