package yeobaek.backend.content.api.metadata;

import java.util.List;
import yeobaek.backend.shared.identity.ContentId;

public interface ContentMetadataResponse {

    interface Creator {

        String name();
    }

    interface Publication {

        Publisher publisher();

        Integer publishedYear();
    }

    interface Publisher {

        String name();
    }

    interface Section {

        long id();

        String title();

        int sequence();

        int startUnitSequence();

        int endUnitSequence();
    }

    ContentId contentId();

    String title();

    List<? extends Creator> creators();

    Publication publication();

    String coverImageUrl();

    int unitCount();
}
