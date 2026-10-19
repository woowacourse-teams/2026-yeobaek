package yeobaek.backend.publicroom.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yeobaek.backend.publicroom.domain.PublicRoom;

public interface PublicRoomRepository extends JpaRepository<PublicRoom, Long> {

    Optional<PublicRoom> findByBookId(Long bookId);

    @Query("""
            select r from PublicRoom r join fetch r.book b
            where b.status = yeobaek.backend.book.domain.BookStatus.ACTIVE
            """)
    List<PublicRoom> findAllActiveWithBook();

    @Query("""
            select r from PublicRoom r join fetch r.book b
            where b.status = yeobaek.backend.book.domain.BookStatus.ACTIVE
              and (b.title.value like concat('%', :keyword, '%')
               or exists (
                   select 1 from AuthorBook ab
                   where ab.book = b and ab.author.name.value like concat('%', :keyword, '%')
               ))
            """)
    List<PublicRoom> searchActiveByTitleOrAuthorName(@Param("keyword") String keyword);

    @Query("select r from PublicRoom r join fetch r.book where r.id = :publicRoomId")
    Optional<PublicRoom> findWithBookById(@Param("publicRoomId") Long publicRoomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PublicRoom r join fetch r.book where r.id = :publicRoomId")
    Optional<PublicRoom> findWithBookByIdForUpdate(@Param("publicRoomId") Long publicRoomId);
}
