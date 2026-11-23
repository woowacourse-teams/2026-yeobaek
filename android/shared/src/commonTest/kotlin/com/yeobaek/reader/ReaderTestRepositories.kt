package com.yeobaek.reader

import com.yeobaek.data.model.BookDetailModel
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.BookSummaryModel
import com.yeobaek.data.model.ChapterModel
import com.yeobaek.data.model.CommentLikeModel
import com.yeobaek.data.model.CommentModel
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.data.model.CommentedSentenceFilter
import com.yeobaek.data.model.CommentedSentencesModel
import com.yeobaek.data.model.CommentsModel
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.NewCommentCountModel
import com.yeobaek.data.model.PassageModel
import com.yeobaek.data.model.PassagesModel
import com.yeobaek.data.model.PublicRoomDetailModel
import com.yeobaek.data.model.PublicRoomModel
import com.yeobaek.data.model.PublicRoomSort
import com.yeobaek.data.model.SentenceModel
import com.yeobaek.data.model.VisitedPublicRoomModel
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.PublicRoomRepository
import com.yeobaek.data.repository.ReaderRepository

internal class ReaderTestRepositories : BookRepository, GroupRepository, ReaderRepository, CommentRepository {
    override suspend fun getBooks(keyword: String): List<BookModel> = error("Not used")
    override suspend fun getBook(bookId: Long): BookModel = error("Not used")
    override suspend fun getBookDetail(bookId: Long) = BookDetailModel(
        authors = listOf("Author"),
        bookId = 11,
        chapters = listOf(ChapterModel(1, 100, 1, 1, "Chapter")),
        passageCount = 100,
        title = "Test book",
        coverImageUrl = null,
        description = null,
        pageCount = null,
    )

    override suspend fun getGroups(): List<GroupModel> = error("Not used")
    override suspend fun createGroup(groupName: String, bookId: Long): Unit = error("Not used")
    override suspend fun joinGroup(joinCode: String): Unit = error("Not used")
    override suspend fun exitGroup(groupId: Long): Unit = error("Not used")
    override suspend fun getGroupDetail(groupId: Long) = GroupDetailModel(
        book = BookSummaryModel(listOf("Author"), 11, 100, "Test book", null),
        groupId = groupId,
        joinCode = "",
        members = emptyList(),
        myProgress = MyProgressModel("2026-11-23", 1, 1),
        name = "Test group",
    )

    override suspend fun getPassages(groupId: Long, from: Int, to: Int) = PassagesModel(
        (from..to).map { sequence ->
            PassageModel(
                chapterId = 1,
                passageId = sequence.toLong(),
                sequence = sequence,
                sentences = listOf(SentenceModel(sequence.toLong(), 1, "Passage $sequence", 0)),
            )
        },
    )

    override suspend fun updatePassage(clubId: Long, passageId: Long): MyProgressModel =
        error("ViewModel must not save directly through ReaderRepository")

    override suspend fun getComments(space: CommentSpace, sentenceId: Long) = CommentsModel(emptyList())
    override suspend fun createComment(space: CommentSpace, sentenceId: Long, content: String): CommentModel =
        error("Not used")
    override suspend fun updateComment(commentId: Long, content: String): CommentModel = error("Not used")
    override suspend fun deleteComment(commentId: Long): Unit = error("Not used")
    override suspend fun setCommentLiked(commentId: Long, liked: Boolean): CommentLikeModel = error("Not used")
    override suspend fun reportComment(commentId: Long): Unit = error("Not used")
    override suspend fun getNewCommentCount(space: CommentSpace, currentPassageId: Long) = NewCommentCountModel(0)
    override suspend fun getCommentedSentences(
        space: CommentSpace,
        currentPassageId: Long,
        filter: CommentedSentenceFilter,
    ): CommentedSentencesModel = error("Not used")
}

internal object UnusedPublicRoomRepository : PublicRoomRepository {
    override suspend fun getPublicRooms(sort: PublicRoomSort?): List<PublicRoomModel> = error("Not used")
    override suspend fun getVisitedPublicRooms(): List<VisitedPublicRoomModel> = error("Not used")
    override suspend fun getPublicRoomDetail(publicRoomId: Long): PublicRoomDetailModel = error("Not used")
    override suspend fun visitPublicRoom(publicRoomId: Long): Unit = error("Not used")
    override suspend fun getPassages(publicRoomId: Long, from: Int, to: Int): PassagesModel = error("Not used")
    override suspend fun updatePassage(publicRoomId: Long, passageId: Long): MyProgressModel = error("Not used")
}
