package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

// @ActiveProfiles("test")
@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationControllerTests extends ControllerTestCase {

  @MockitoBean UCSBOrganizationRepository ucsbOrganizationRepository;

  @MockitoBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/UCSBOrganization/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/UCSBOrganization/all")).andExpect(status().is(200)); // logged
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/UCSBOrganization/post")
                .param("orgCode", "fff")
                .param("orgTranslationShort", "F")
                .param("orgTranslation", "Fail")
                .param("inactive", "true")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/UCSBOrganization/post")
                .param("orgCode", "fff")
                .param("orgTranslationShort", "F")
                .param("orgTranslation", "Fail")
                .param("inactive", "true")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_ucsborganizations() throws Exception {

    // arrange

    UCSBOrganization VSA =
        UCSBOrganization.builder()
            .orgCode("vsa")
            .orgTranslationShort("VSA")
            .orgTranslation("Vietanamese Student Association")
            .inactive(false)
            .build();

    UCSBOrganization TT =
        UCSBOrganization.builder()
            .orgCode("tt")
            .orgTranslationShort("TT")
            .orgTranslation("Theta Tau")
            .inactive(false)
            .build();

    ArrayList<UCSBOrganization> expectedOrganizations = new ArrayList<>();
    expectedOrganizations.addAll(Arrays.asList(VSA, TT));

    when(ucsbOrganizationRepository.findAll()).thenReturn(expectedOrganizations);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/UCSBOrganization/all")).andExpect(status().isOk()).andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedOrganizations);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_organization() throws Exception {

    // Return the entity that the controller actually passes to save()
    when(ucsbOrganizationRepository.save(any(UCSBOrganization.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ArgumentCaptor<UCSBOrganization> captor = ArgumentCaptor.forClass(UCSBOrganization.class);

    // Use inactive=true (NOT the default false)
    mockMvc
        .perform(
            post("/api/UCSBOrganization/post")
                .param("orgCode", "CSU")
                .param("orgTranslationShort", "CSU")
                .param("orgTranslation", "Chinese Student Union")
                .param("inactive", "true")
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orgCode").value("CSU"))
        .andExpect(jsonPath("$.orgTranslationShort").value("CSU"))
        .andExpect(jsonPath("$.orgTranslation").value("Chinese Student Union"))
        .andExpect(jsonPath("$.inactive").value(true));

    verify(ucsbOrganizationRepository, times(1)).save(captor.capture());
    UCSBOrganization savedArg = captor.getValue();

    assertTrue(
        savedArg.getInactive()); // If setInactive is removed, this becomes false -> test fails
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/UCSBOrganization").param("orgCode", "123"))
        .andExpect(status().is(403)); // logged out users can't get by id
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange

    UCSBOrganization org =
        UCSBOrganization.builder()
            .orgCode("tt")
            .orgTranslationShort("TT")
            .orgTranslation("Theta Tau")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.findById(eq("tt"))).thenReturn(Optional.of(org));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBOrganization").param("orgCode", "tt"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findById(eq("tt"));
    String expectedJson = mapper.writeValueAsString(org);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(ucsbOrganizationRepository.findById(eq("123"))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBOrganization").param("orgCode", "123"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(ucsbOrganizationRepository, times(1)).findById(eq("123"));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id 123 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_an_organization() throws Exception {
    // arrange

    UCSBOrganization org =
        UCSBOrganization.builder()
            .orgCode("tt")
            .orgTranslationShort("TT")
            .orgTranslation("Theta Tau")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.findById(eq("tt"))).thenReturn(Optional.of(org));

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/UCSBOrganization").param("orgCode", "tt").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).findById("tt");
    verify(ucsbOrganizationRepository, times(1)).delete(any());

    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBOrganization with id tt deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_tries_to_delete_non_existant_organization_and_gets_right_error_message()
      throws Exception {
    // arrange

    when(ucsbOrganizationRepository.findById(eq("munger-hall"))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(delete("/api/UCSBOrganization").param("orgCode", "munger-hall").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).findById("munger-hall");
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBOrganization with id munger-hall not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_organization() throws Exception {
    // arrange

    UCSBOrganization orgOrig =
        UCSBOrganization.builder()
            .orgCode("tt")
            .orgTranslationShort("TT")
            .orgTranslation("Theta Tau")
            .inactive(false)
            .build();

    UCSBOrganization orgEdited =
        UCSBOrganization.builder()
            .orgCode("vsa")
            .orgTranslationShort("VSA")
            .orgTranslation("Vietamese Student Association")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(orgEdited);

    when(ucsbOrganizationRepository.findById(eq("tt"))).thenReturn(Optional.of(orgOrig));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/UCSBOrganization")
                    .param("orgCode", "tt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).findById("tt");
    verify(ucsbOrganizationRepository, times(1))
        .save(orgEdited); // should be saved with updated info
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_organization_that_does_not_exist() throws Exception {
    // arrange

    UCSBOrganization editedOrganization =
        UCSBOrganization.builder()
            .orgCode("tt")
            .orgTranslationShort("TT")
            .orgTranslation("Triangle Triangle")
            .inactive(true)
            .build();

    String requestBody = mapper.writeValueAsString(editedOrganization);

    when(ucsbOrganizationRepository.findById(eq("tt"))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/UCSBOrganization")
                    .param("orgCode", "tt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).findById("tt");
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBOrganization with id tt not found", json.get("message"));
  }
}
