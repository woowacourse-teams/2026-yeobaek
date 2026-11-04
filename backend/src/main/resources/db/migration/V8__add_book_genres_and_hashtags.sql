CREATE TABLE book_genres (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_book_genres_book (book_id),
    CONSTRAINT fk_book_genres_book FOREIGN KEY (book_id) REFERENCES books (id)
) CHARACTER SET utf8mb4;

CREATE TABLE book_hashtags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    value VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_book_hashtags_book (book_id),
    CONSTRAINT fk_book_hashtags_book FOREIGN KEY (book_id) REFERENCES books (id)
) CHARACTER SET utf8mb4;
