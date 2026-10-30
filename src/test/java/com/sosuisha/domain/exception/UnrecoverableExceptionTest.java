package com.sosuisha.domain.exception;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnrecoverableExceptionTest {
    @Test
    @DisplayName("回復できないエラーは、空白だけのメッセージでは作れない")
    void an_unrecoverable_error_cannot_be_made_with_a_blank_message() {
        assertThrows(IllegalArgumentException.class, () -> new RepositoryException(" ", null));
    }
}
