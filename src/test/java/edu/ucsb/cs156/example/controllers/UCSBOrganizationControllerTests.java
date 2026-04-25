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
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
    // arrange

    UCSBOrganization csu =
        UCSBOrganization.builder()
            .orgCode("CSU")
            .orgTranslationShort("CSU")
            .orgTranslation("Chinese Student Union")
            .inactive(false)
            .build();

    when(ucsbOrganizationRepository.save(eq(csu))).thenReturn(csu);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/UCSBOrganization/post")
                    .param("orgCode", "CSU")
                    .param("orgTranslationShort", "CSU")
                    .param("orgTranslation", "Chinese Student Union")
                    .param("inactive", "false")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbOrganizationRepository, times(1)).save(csu);
    String expectedJson = mapper.writeValueAsString(csu);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
