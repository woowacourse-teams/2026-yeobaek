package yeobaek.backend.readmodel.admin;

import java.util.List;
import java.util.Map;

public interface AdminClubStatisticsReadModel {

    Map<Long, Long> countComments(List<Long> clubIds);

    Map<Long, Long> countClubsByBookIds(List<Long> bookIds);
}
