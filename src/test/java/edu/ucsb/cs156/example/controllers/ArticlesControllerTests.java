package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Articles;
import edu.ucsb.cs156.example.repositories.ArticlesRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = ArticlesController.class)
@Import(TestConfig.class)
public class ArticlesControllerTests extends ControllerTestCase {

  @MockitoBean ArticlesRepository articlesRepository;

  @MockitoBean UserRepository userRepository;

  // Tests for GET /api/Articles/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/Articles/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/Articles/all")).andExpect(status().is(200));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_articles() throws Exception {

    LocalDateTime ldt1 = LocalDateTime.parse("2022-04-20T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-04-21T00:00:00");

    Articles article1 =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url(
                "https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    Articles article2 =
        Articles.builder()
            .title("Handy Spring Utility Classes")
            .url("https://twitter.com/maciejwalkowiak/status/1511736828369719300")
            .explanation("A useful reference")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt2)
            .build();

    ArrayList<Articles> expectedArticles = new ArrayList<>(Arrays.asList(article1, article2));

    when(articlesRepository.findAll()).thenReturn(expectedArticles);

    MvcResult response =
        mockMvc.perform(get("/api/Articles/all")).andExpect(status().isOk()).andReturn();

    verify(articlesRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedArticles);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/Articles").param("id", "7")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    LocalDateTime ldt = LocalDateTime.parse("2022-04-20T00:00:00");

    Articles article =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url(
                "https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt)
            .build();

    when(articlesRepository.findById(eq(7L))).thenReturn(Optional.of(article));

    MvcResult response =
        mockMvc
            .perform(get("/api/Articles").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    verify(articlesRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(article);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    when(articlesRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/Articles").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(articlesRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Articles with id 7 not found", json.get("message"));
  }

  // Tests for POST /api/Articles/post

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(post("/api/Articles/post")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/Articles/post")
                .param("title", "Using testing-playground with React Testing Library")
                .param(
                    "url",
                    "https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
                .param("explanation", "Helpful when we get to front end development")
                .param("email", "phtcon@ucsb.edu")
                .param("dateAdded", "2022-04-20T00:00:00")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_article() throws Exception {

    LocalDateTime ldt = LocalDateTime.parse("2022-04-20T00:00:00");

    Articles article =
        Articles.builder()
            .title("Using testing-playground with React Testing Library")
            .url(
                "https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
            .explanation("Helpful when we get to front end development")
            .email("phtcon@ucsb.edu")
            .dateAdded(ldt)
            .build();

    when(articlesRepository.save(eq(article))).thenReturn(article);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/Articles/post")
                    .param("title", "Using testing-playground with React Testing Library")
                    .param(
                        "url",
                        "https://dev.to/katieraby/using-testing-playground-with-react-testing-library-26j7")
                    .param("explanation", "Helpful when we get to front end development")
                    .param("email", "phtcon@ucsb.edu")
                    .param("dateAdded", "2022-04-20T00:00:00")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(articlesRepository, times(1)).save(eq(article));
    String expectedJson = mapper.writeValueAsString(article);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
