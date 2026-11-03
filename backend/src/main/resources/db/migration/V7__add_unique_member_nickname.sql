ALTER TABLE members
    ADD CONSTRAINT uk_members_nickname UNIQUE (nickname);
