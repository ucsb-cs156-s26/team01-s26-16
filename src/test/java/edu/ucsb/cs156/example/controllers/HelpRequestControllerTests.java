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
import edu.ucsb.cs156.example.entities.HelpRequest;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;
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

@WebMvcTest(controllers = HelpRequestController.class)
@Import(TestConfig.class)
public class HelpRequestControllerTests extends ControllerTestCase {

  @MockitoBean HelpRequestRepository helpRequestRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization tests for /api/helprequests/admin/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/helprequests/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/helprequests/all")).andExpect(status().is(200)); // logged
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_helprequests() throws Exception {

    // arrange
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    HelpRequest helpRequest1 =
        HelpRequest.builder()
            .requesterEmail("test@test.edu")
            .teamId("testTeam")
            .tableOrBreakoutRoom("testTable")
            .requestTime(ldt1)
            .explanation("testExplanation")
            .solved(true)
            .build();

    LocalDateTime ldt2 = LocalDateTime.parse("2022-03-11T00:00:00");

    HelpRequest helpRequest2 =
        HelpRequest.builder()
            .requesterEmail("test2@test.edu")
            .teamId("testTeam2")
            .tableOrBreakoutRoom("testTable2")
            .requestTime(ldt2)
            .explanation("testExplanation2")
            .solved(false)
            .build();

    ArrayList<HelpRequest> expectedRequests = new ArrayList<>();
    expectedRequests.addAll(Arrays.asList(helpRequest1, helpRequest2));

    when(helpRequestRepository.findAll()).thenReturn(expectedRequests);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/helprequests/all")).andExpect(status().isOk()).andReturn();

    // assert

    verify(helpRequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedRequests);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/helprequests").param("id", "7"))
        .andExpect(status().is(403)); // logged out users can't get by id
  }

  // Authorization tests for /api/ucsbdates/post
  // (Perhaps should also have these for put and delete)

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/helprequests/post")
                .param("requesterEmail", "test@test.edu")
                .param("teamId", "testTeam")
                .param("tableOrBreakoutRoom", "testTable")
                .param("requestTime", "2022-01-03T00:00:00")
                .param("explanation", "testExplanation")
                .param("solved", "true")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/helprequests/post")
                .param("requesterEmail", "test@test.edu")
                .param("teamId", "testTeam")
                .param("tableOrBreakoutRoom", "testTable")
                .param("requestTime", "2022-01-03T00:00:00")
                .param("explanation", "testExplanation")
                .param("solved", "true")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange
    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

    HelpRequest helpRequest =
        HelpRequest.builder()
            .requesterEmail("test@test.edu")
            .teamId("testTeam")
            .tableOrBreakoutRoom("testTable")
            .requestTime(ldt)
            .explanation("testExplanation")
            .solved(true)
            .build();

    when(helpRequestRepository.findById(eq(7L))).thenReturn(Optional.of(helpRequest));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/helprequests").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(helpRequestRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(helpRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(helpRequestRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/helprequests").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(helpRequestRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("HelpRequest with id 7 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_helprequest() throws Exception {
    // arrange

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    HelpRequest helpRequest =
        HelpRequest.builder()
            .requesterEmail("test@test.edu")
            .teamId("testTeam")
            .tableOrBreakoutRoom("testTable")
            .requestTime(ldt1)
            .explanation("testExplanation")
            .solved(true)
            .build();

    when(helpRequestRepository.save(eq(helpRequest))).thenReturn(helpRequest);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/helprequests/post")
                    .param("requesterEmail", "test@test.edu")
                    .param("teamId", "testTeam")
                    .param("tableOrBreakoutRoom", "testTable")
                    .param("requestTime", "2022-01-03T00:00:00")
                    .param("explanation", "testExplanation")
                    .param("solved", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(helpRequestRepository, times(1)).save(helpRequest);
    String expectedJson = mapper.writeValueAsString(helpRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
