--liquibase formatted sql
--changeset oleh:1
ALTER TABLE posts SET COMMENT="A Test Remark";
--rollback ALTER TABLE posts UNSET COMMENT;

