package org.bibliotecaviva.backend.integration;

import org.bibliotecaviva.backend.domain.entities.Comment;
import org.bibliotecaviva.backend.domain.entities.CommentReply;
import org.bibliotecaviva.backend.domain.entities.News;
import org.bibliotecaviva.backend.domain.entities.User;
import org.bibliotecaviva.backend.domain.entities.textual.Article;
import org.bibliotecaviva.backend.domain.enums.Role;
import org.bibliotecaviva.backend.domain.enums.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void curatorShouldListRegisteredProfilesWithStatusFilter() throws Exception {
        User curator = createActiveCurator();
        User target = createActiveStudent();

        mockMvc.perform(get("/user")
                        .header("Authorization", bearer(curator))
                        .queryParam("status", "ACTIVE")
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]", target.getId()).exists());
    }

    @Test
    void studentShouldNotListRegisteredProfiles() throws Exception {
        User student = createActiveStudent();

        mockMvc.perform(get("/user").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldFindUserByEmail() throws Exception {
        User admin = createActiveAdmin();
        User target = createActiveStudent();

        mockMvc.perform(get("/user/find-by-email")
                        .header("Authorization", bearer(admin))
                        .queryParam("email", target.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId().toString()))
                .andExpect(jsonPath("$.name").value(target.getName()))
                .andExpect(jsonPath("$.email").value(target.getEmail()))
                .andExpect(jsonPath("$.role").value(Role.ALUNO.name()))
                .andExpect(jsonPath("$.accountStatus").value("active"));
    }

    @Test
    void curatorShouldFindUserByEmail() throws Exception {
        User curator = createActiveCurator();
        User target = createUser("Admin buscado", uniqueEmail("admin-buscado"), Role.ADMIN, Status.ACTIVE);

        mockMvc.perform(get("/user/find-by-email")
                        .header("Authorization", bearer(curator))
                        .queryParam("email", target.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId().toString()))
                .andExpect(jsonPath("$.email").value(target.getEmail()))
                .andExpect(jsonPath("$.role").value(Role.ADMIN.name()))
                .andExpect(jsonPath("$.accountStatus").value("active"));
    }

    @Test
    void findUserByEmailShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        User admin = createActiveAdmin();

        mockMvc.perform(get("/user/find-by-email")
                        .header("Authorization", bearer(admin))
                        .queryParam("email", uniqueEmail("ausente")))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentShouldNotFindUserByEmail() throws Exception {
        User student = createActiveStudent();
        User target = createActiveCurator();

        mockMvc.perform(get("/user/find-by-email")
                        .header("Authorization", bearer(student))
                        .queryParam("email", target.getEmail()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldDeleteUserWithNewsAndPreserveNewsWithNullAuthor() throws Exception {
        User admin = createActiveAdmin();
        User curator = createActiveCurator();
        News news = createNewsInDatabase(curator, uniqueTitle("Noticia"));
        flushAndClear();

        mockMvc.perform(delete("/admin/users/{id}", curator.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        flushAndClear();

        assertTrue(userRepository.findById(curator.getId()).isEmpty());
        News persistedNews = newsRepository.findById(news.getId()).orElseThrow();
        assertNull(persistedNews.getAuthor());
    }

    @Test
    void adminShouldDeleteUserWithRepliesToThirdPartyComments() throws Exception {
        User admin = createActiveAdmin();
        User curator = createActiveCurator();
        User student = createActiveStudent();
        Article work = createArticleInDatabase(admin, uniqueTitle("Obra"));
        Comment studentComment = createCommentInDatabase(student, work, "Comentario de aluno");
        CommentReply curatorReply = createCommentReplyInDatabase(curator, studentComment, "Resposta do curador");
        flushAndClear();

        mockMvc.perform(delete("/admin/users/{id}", curator.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        flushAndClear();

        assertTrue(userRepository.findById(curator.getId()).isEmpty());
        assertTrue(commentReplyRepository.findById(curatorReply.getId()).isEmpty());
        assertTrue(commentRepository.findById(studentComment.getId()).isPresent());
    }
}
