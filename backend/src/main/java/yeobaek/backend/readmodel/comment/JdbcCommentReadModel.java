package yeobaek.backend.readmodel.comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;

@Component
@RequiredArgsConstructor
public class JdbcCommentReadModel implements CommentReadModel {

    private static final String CONTENT_ID = "content_id";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<SpaceContentSnapshot> findClub(Long clubId) {
        return jdbcTemplate.query("""
                select c.id, c.space_id, c.name, c.join_code, b.content_id, b.status
                from clubs c join space_content_bindings binding on binding.space_id = c.space_id
                join books b on b.content_id = binding.content_id where c.id = ?
                """, (resultSet, rowNumber) -> new SpaceContentSnapshot(
                    new yeobaek.backend.space.domain.Club(new SpaceId(resultSet.getLong("space_id")),
                            resultSet.getLong("id"), resultSet.getString("name"),
                            resultSet.getString("join_code")),
                    new ContentId(resultSet.getLong(CONTENT_ID)),
                    "ACTIVE".equals(resultSet.getString("status"))), clubId).stream().findFirst();
    }

    @Override
    public Optional<SpaceContentSnapshot> findPublicRoom(Long publicRoomId) {
        return jdbcTemplate.query("""
                select r.id, r.space_id, b.content_id, b.status
                from public_rooms r join space_content_bindings binding on binding.space_id = r.space_id
                join books b on b.content_id = binding.content_id where r.id = ?
                """, (resultSet, rowNumber) -> new SpaceContentSnapshot(
                    new yeobaek.backend.space.domain.PublicRoom(new SpaceId(resultSet.getLong("space_id")),
                            resultSet.getLong("id")),
                    new ContentId(resultSet.getLong(CONTENT_ID)),
                    "ACTIVE".equals(resultSet.getString("status"))), publicRoomId).stream().findFirst();
    }

    @Override
    public Optional<LocationSnapshot> findSentence(Long sentenceId) {
        return jdbcTemplate.query("""
                select s.location_id, l.content_id, p.sequence
                from sentences s join content_locations l on l.id = s.location_id
                join passages p on p.id = s.passage_id where s.id = ?
                """, (resultSet, rowNumber) -> new LocationSnapshot(
                    new ContentLocationId(resultSet.getLong("location_id")),
                    new ContentId(resultSet.getLong(CONTENT_ID)), resultSet.getInt("sequence")),
                sentenceId).stream().findFirst();
    }

    @Override
    public Optional<LocationSnapshot> findPassage(Long passageId) {
        return jdbcTemplate.query("""
                select p.location_id, l.content_id, p.sequence
                from passages p join content_locations l on l.id = p.location_id where p.id = ?
                """, (resultSet, rowNumber) -> new LocationSnapshot(
                    new ContentLocationId(resultSet.getLong("location_id")),
                    new ContentId(resultSet.getLong(CONTENT_ID)), resultSet.getInt("sequence")),
                passageId).stream().findFirst();
    }

    @Override
    public List<Comment> findVisible(MemberId requesterId, SpaceId spaceId, ContentLocationId locationId) {
        return jdbcTemplate.query("""
                select c.id, a.author_id, c.content, a.created_at, a.updated_at
                from comments c join appreciations a on a.id = c.id
                join appreciation_contexts ctx on ctx.appreciation_id = c.id
                where ctx.space_id = ? and ctx.location_id = ? and not exists (
                    select mb.id from member_blocks mb
                    where mb.blocker_id = ? and mb.blocked_id = a.author_id)
                order by a.created_at asc, c.id asc
                """, (resultSet, rowNumber) -> new Comment(
                    new AppreciationId(resultSet.getLong("id")),
                    new MemberId(resultSet.getLong("author_id")),
                    resultSet.getString("content"),
                    resultSet.getObject("created_at", LocalDateTime.class),
                    resultSet.getObject("updated_at", LocalDateTime.class)),
                spaceId.value(), locationId.value(), requesterId.value());
    }

    @Override
    public long countNewVisible(MemberId requesterId, SpaceId spaceId, int currentPassageSequence) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*) from comments c join appreciations a on a.id = c.id
                join appreciation_contexts ctx on ctx.appreciation_id = c.id
                join sentences s on s.location_id = ctx.location_id
                join passages p on p.id = s.passage_id
                where ctx.space_id = ? and p.sequence <= ?
                  and not exists (select v.id from appreciation_views v
                    where v.actor_id = ? and v.appreciation_id = c.id)
                  and not exists (select mb.id from member_blocks mb
                    where mb.blocker_id = ? and mb.blocked_id = a.author_id)
                """, Long.class, spaceId.value(), currentPassageSequence,
                requesterId.value(), requesterId.value());
        return count == null ? 0L : count;
    }

    @Override
    public List<DiscoverySnapshot> findDiscovery(MemberId requesterId, SpaceId spaceId) {
        return jdbcTemplate.query("""
                select s.id as sentence_id, s.content, p.id as passage_id,
                       p.sequence as passage_sequence, s.sequence as sentence_sequence,
                       count(distinct c.id) as comment_count,
                       count(distinct case when v.id is null then c.id end) as unread_comment_count,
                       max(a.created_at) as latest_comment_created_at
                from comments c join appreciations a on a.id = c.id
                join appreciation_contexts ctx on ctx.appreciation_id = c.id
                join sentences s on s.location_id = ctx.location_id
                join passages p on p.id = s.passage_id
                left join appreciation_views v on v.appreciation_id = c.id and v.actor_id = ?
                where ctx.space_id = ? and not exists (
                    select mb.id from member_blocks mb
                    where mb.blocker_id = ? and mb.blocked_id = a.author_id)
                group by s.id, s.content, p.id, p.sequence, s.sequence
                """, (resultSet, rowNumber) -> new DiscoverySnapshot(
                    resultSet.getLong("sentence_id"), resultSet.getString("content"),
                    resultSet.getLong("passage_id"), resultSet.getInt("passage_sequence"),
                    resultSet.getInt("sentence_sequence"), resultSet.getLong("comment_count"),
                    resultSet.getLong("unread_comment_count"),
                    resultSet.getObject("latest_comment_created_at", LocalDateTime.class)),
                requesterId.value(), spaceId.value(), requesterId.value());
    }

}
