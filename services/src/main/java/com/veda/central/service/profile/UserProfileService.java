/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.veda.central.service.profile;

import com.veda.central.core.mapper.user.AttributeUpdateMetadataMapper;
import com.veda.central.core.mapper.user.GroupMapper;
import com.veda.central.core.mapper.user.StatusUpdateMetadataMapper;
import com.veda.central.core.mapper.user.UserProfileMapper;
import com.veda.central.core.model.user.AttributeUpdateMetadata;
import com.veda.central.core.model.user.Group;
import com.veda.central.core.model.user.GroupToGroupMembership;
import com.veda.central.core.model.user.StatusUpdateMetadata;
import com.veda.central.core.model.user.UserGroupMembership;
import com.veda.central.core.model.user.UserGroupMembershipType;
import com.veda.central.core.model.user.UserProfile;
import com.veda.central.core.repo.user.AttributeUpdateMetadataRepository;
import com.veda.central.core.repo.user.GroupAttributeRepository;
import com.veda.central.core.repo.user.GroupMembershipRepository;
import com.veda.central.core.repo.user.GroupMembershipTypeRepository;
import com.veda.central.core.repo.user.GroupRepository;
import com.veda.central.core.repo.user.GroupRoleRepository;
import com.veda.central.core.repo.user.GroupToGroupMembershipRepository;
import com.veda.central.core.repo.user.StatusUpdateMetadataRepository;
import com.veda.central.core.repo.user.UserAttributeRepository;
import com.veda.central.core.repo.user.UserProfileRepository;
import com.veda.central.core.repo.user.UserRoleRepository;
import com.veda.central.core.user.profile.api.DefaultGroupMembershipTypes;
import com.veda.central.core.user.profile.api.GetAllGroupsResponse;
import com.veda.central.core.user.profile.api.GetAllUserProfilesResponse;
import com.veda.central.core.user.profile.api.GetUpdateAuditTrailRequest;
import com.veda.central.core.user.profile.api.GetUpdateAuditTrailResponse;
import com.veda.central.core.user.profile.api.GroupMembership;
import com.veda.central.core.user.profile.api.GroupRequest;
import com.veda.central.core.user.profile.api.Status;
import com.veda.central.core.user.profile.api.UserAttribute;
import com.veda.central.core.user.profile.api.UserGroupMembershipTypeRequest;
import com.veda.central.core.user.profile.api.UserProfileAttributeUpdateMetadata;
import com.veda.central.core.user.profile.api.UserProfileRequest;
import com.veda.central.core.user.profile.api.UserProfileStatusUpdateMetadata;
import com.veda.central.service.exceptions.InternalServerException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private StatusUpdateMetadataRepository statusUpdaterRepository;

    @Autowired
    private AttributeUpdateMetadataRepository attributeUpdateMetadataRepository;

    @Autowired
    private UserAttributeRepository userAttributeRepository;

    @Autowired
    private UserRoleRepository roleRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupRoleRepository groupRoleRepository;

    @Autowired
    private GroupAttributeRepository groupAttributeRepository;

    @Autowired
    private GroupMembershipRepository groupMembershipRepository;

    @Autowired
    private GroupToGroupMembershipRepository groupToGroupMembershipRepository;

    @Autowired
    private GroupMembershipTypeRepository groupMembershipTypeRepository;


    public com.veda.central.core.user.profile.api.UserProfile createUserProfile(UserProfileRequest request) {
        try {
            LOGGER.debug("Request received to createUserProfile for " + request.getProfile().getUsername() + "at " + request.getTenantId());

            String userId = request.getProfile().getUsername() + "@" + request.getTenantId();

            Optional<UserProfile> op = repository.findById(userId);

            if (op.isEmpty()) {
                UserProfile entity = UserProfileMapper.createUserProfileEntityFromUserProfile(request.getProfile());
                entity.setId(userId);
                entity.setTenantId(request.getTenantId());
                repository.save(entity);
            }

            return request.getProfile();

        } catch (Exception ex) {
            String msg = "Error occurred while creating user profile for " + request.getProfile().getUsername() + "at "
                    + request.getTenantId() + " reason :" + ex.getMessage();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public com.veda.central.core.user.profile.api.UserProfile updateUserProfile(UserProfileRequest request) {
        try {
            LOGGER.debug("Request received to updateUserProfile for " + request.getProfile().getUsername() + "at " + request.getTenantId());

            String userId = request.getProfile().getUsername() + "@" + request.getTenantId();

            Optional<UserProfile> exEntity = repository.findById(userId);


            if (exEntity.isPresent()) {
                UserProfile entity = UserProfileMapper.createUserProfileEntityFromUserProfile(request.getProfile());
                Set<AttributeUpdateMetadata> metadata = AttributeUpdateMetadataMapper.
                        createAttributeUpdateMetadataEntity(exEntity.get(), entity, request.getPerformedBy());

                entity.setAttributeUpdateMetadata(metadata);
                entity.setId(userId);
                entity.setTenantId(request.getTenantId());
                entity.setCreatedAt(exEntity.get().getCreatedAt());

                UserProfile exProfile = exEntity.get();

                if (exProfile.getUserAttribute() != null) {
                    userAttributeRepository.deleteAll(exProfile.getUserAttribute());
                }

                if (exProfile.getUserRole() != null) {
                    roleRepository.deleteAll(exProfile.getUserRole());
                }

                repository.save(entity);
                return request.getProfile();

            } else {
                LOGGER.error("Cannot find a user profile for " + userId);
                throw new EntityNotFoundException("Cannot find a user profile for " + userId);
            }

        } catch (Exception ex) {
            String msg = "Error occurred while updating user profile for " + request.getProfile().getUsername() + "at "
                    + request.getTenantId() + " reason :" + ex.getMessage();
            LOGGER.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    public com.veda.central.core.user.profile.api.UserProfile getUserProfile(UserProfileRequest request) {
        try {
            LOGGER.debug("Request received to getUserProfile for " + request.getProfile().getUsername() + "at " + request.getTenantId());

            String userId = request.getProfile().getUsername() + "@" + request.getTenantId();

            Optional<UserProfile> entity = repository.findById(userId);

            if (entity.isPresent()) {
                UserProfile profileEntity = entity.get();
                return UserProfileMapper.createUserProfileFromUserProfileEntity(profileEntity, null);

            } else {
                throw new EntityNotFoundException("Could not find the UserProfile with the id: " + userId);
            }

        } catch (Exception ex) {
            String msg = "Error occurred while fetching user profile for " + request.getProfile().getUsername() + "at " + request.getTenantId();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public com.veda.central.core.user.profile.api.UserProfile deleteUserProfile(UserProfileRequest request) {
        try {
            LOGGER.debug("Request received to deleteUserProfile for " + request.getProfile().getUsername() + "at " + request.getTenantId());
            long tenantId = request.getTenantId();

            String username = request.getProfile().getUsername();

            String userId = username + "@" + tenantId;

            Optional<UserProfile> profileEntity = repository.findById(userId);
            if (profileEntity.isPresent()) {
                UserProfile entity = profileEntity.get();
                com.veda.central.core.user.profile.api.UserProfile profile = UserProfileMapper.createUserProfileFromUserProfileEntity(entity, null);
                repository.delete(entity);
                return profile;

            } else {
                throw new EntityNotFoundException("Could not find the UserProfile with the id: " + userId);
            }

        } catch (Exception ex) {
            String msg = "Error occurred while deleting user profile for " + request.getProfile().getUsername() + "at " + request.getTenantId();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public GetAllUserProfilesResponse getAllUserProfilesInTenant(UserProfileRequest request) {
        try {
            LOGGER.debug("Request received to getAllUserProfilesInTenant for " + request.getTenantId());
            long tenantId = request.getTenantId();
            int limit = request.getLimit();
            int offset = request.getOffset();

            List<UserProfile> profileList;

            if (limit > 0) {
                profileList = repository.findByTenantIdWithPagination(tenantId, limit, offset);
            } else {
                profileList = repository.findByTenantId(tenantId);
            }

            List<com.veda.central.core.user.profile.api.UserProfile> userProfileList = null;

            if (profileList != null && !profileList.isEmpty()) {
                userProfileList = profileList.stream()
                        .map(entity -> UserProfileMapper.createUserProfileFromUserProfileEntity(entity, null))
                        .toList();
            }

            return GetAllUserProfilesResponse
                    .newBuilder()
                    .addAllProfiles(userProfileList)
                    .build();

        } catch (Exception ex) {
            String msg = "Error occurred while fetching  user profile for tenant " + request.getTenantId();
            LOGGER.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    public GetAllUserProfilesResponse findUserProfilesByAttributes(UserProfileRequest request) {
        try {
            LOGGER.debug("Request received to findUserProfilesByAttributes at " + request.getTenantId());

            List<UserAttribute> attributeList = request.getProfile().getAttributesList();
            List<UserProfile> selectedProfiles = new ArrayList<>();
            List<com.veda.central.core.user.profile.api.UserProfile> userProfileList = new ArrayList<>();

            attributeList.forEach(atr -> {
                List<String> values = atr.getValuesList();
                values.forEach(val -> {
                    List<UserProfile> userAttributes = userAttributeRepository.findFilteredUserProfiles(atr.getKey(), val);
                    if (userAttributes == null || userAttributes.isEmpty()) {
                        return;
                    }

                    if (selectedProfiles.isEmpty()) {
                        selectedProfiles.addAll(userAttributes);
                    } else {
                        List<UserProfile> profiles = userAttributes.stream().filter(newProf -> {
                            AtomicBoolean matched = new AtomicBoolean(false);
                            selectedProfiles.forEach(selectedProfile -> {
                                if (selectedProfile.getId().equals(newProf.getId())) {
                                    matched.set(true);
                                }
                            });
                            return matched.get();
                        }).toList();
                        selectedProfiles.clear();
                        selectedProfiles.addAll(profiles);
                    }
                });
            });

            if (!selectedProfiles.isEmpty()) {
                selectedProfiles.forEach(userProfile -> {
                    com.veda.central.core.user.profile.api.UserProfile prof = UserProfileMapper.createUserProfileFromUserProfileEntity(userProfile, null);
                    userProfileList.add(prof);
                });
                return GetAllUserProfilesResponse.newBuilder().addAllProfiles(userProfileList).build();

            } else {
                return GetAllUserProfilesResponse.newBuilder().build();
            }

        } catch (Exception ex) {
            String msg = "Error occurred while fetching user profile for " + request.getProfile().getUsername() + "at " + request.getTenantId();
            LOGGER.error(msg);
            throw new InternalServerException(msg, ex);
        }
    }

    public GetUpdateAuditTrailResponse getUserProfileAuditTrails(GetUpdateAuditTrailRequest request) {
        try {
            LOGGER.debug("Request received to getUserProfileAuditTrails for " + request.getUsername() + "at " + request.getTenantId());

            String username = request.getUsername();
            long tenantId = request.getTenantId();
            String userId = username + "@" + tenantId;

            List<StatusUpdateMetadata> statusUpdateMetadata = statusUpdaterRepository.findAllByUserProfileId(userId);
            List<AttributeUpdateMetadata> attributeUpdateMetadata = attributeUpdateMetadataRepository.findAllByUserProfileId(userId);

            List<UserProfileStatusUpdateMetadata> userProfileStatusUpdateMetadata = null;
            List<UserProfileAttributeUpdateMetadata> userProfileAttributeUpdateMetadata = null;

            if (statusUpdateMetadata != null && !statusUpdateMetadata.isEmpty()) {
                userProfileStatusUpdateMetadata = statusUpdateMetadata.stream()
                        .map(StatusUpdateMetadataMapper::createUserProfileStatusMetadataFrom)
                        .toList();
            }

            if (attributeUpdateMetadata != null && !attributeUpdateMetadata.isEmpty()) {
                userProfileAttributeUpdateMetadata = attributeUpdateMetadata.stream()
                        .map(AttributeUpdateMetadataMapper::createAttributeUpdateMetadataFromEntity)
                        .toList();
            }

            return GetUpdateAuditTrailResponse
                    .newBuilder()
                    .addAllAttributeAudit(userProfileAttributeUpdateMetadata)
                    .addAllStatusAudit(userProfileStatusUpdateMetadata)
                    .build();

        } catch (Exception ex) {
            String msg = "Error occurred while fetching  audit trials " + request.getUsername() + "at " + request.getTenantId();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public com.veda.central.core.user.profile.api.Group createGroup(GroupRequest request) {
        try {
            LOGGER.debug("Request received to createGroup from tenant" + request.getTenantId());

            String groupId = request.getGroup().getId();
            long tenantId = request.getTenantId();

            String effectiveId = groupId + "@" + tenantId;

            Optional<Group> op = groupRepository.findById(effectiveId);

            String ownerId = request.getGroup().getOwnerId() + "@" + tenantId;

            Optional<UserProfile> userProfile = repository.findById(ownerId);

            if (userProfile.isEmpty()) {
                String msg = "Error occurred while creating  Group for " + request.getTenantId() + " reason : Owner  not found";
                LOGGER.error(msg);
                throw new RuntimeException(msg);
            }

            Group savedGroup = null;
            if (op.isEmpty()) {

                Group entity = GroupMapper.createGroupEntity(request.getGroup(), request.getTenantId());

                String parentId = entity.getParentId();
                if (parentId != null && !parentId.trim().isEmpty()) {
                    Optional<Group> parent = groupRepository.findById(parentId);

                    if (parent.isEmpty()) {
                        String msg = "Error occurred while creating  Group for " + request.getTenantId() + " reason : Parent group not found";
                        LOGGER.error(msg);
                        throw new RuntimeException(msg);
                    }
                    GroupMapper.setParentGroupMembership(parent.get(), entity);
                }
                savedGroup = groupRepository.save(entity);
            }

            Optional<Group> exOP = groupRepository.findById(effectiveId);

            if (exOP.isPresent()) {
                String type = DefaultGroupMembershipTypes.OWNER.name();

                Optional<UserGroupMembershipType> groupMembershipType = groupMembershipTypeRepository.findById(type);
                UserGroupMembershipType exist;

                if (groupMembershipType.isEmpty()) {
                    exist = new UserGroupMembershipType();
                    exist.setId(type);
                    groupMembershipTypeRepository.save(exist);

                } else {
                    exist = groupMembershipType.get();
                }

                UserGroupMembership userGroupMembership = new UserGroupMembership();
                userGroupMembership.setGroup(savedGroup);
                userGroupMembership.setUserProfile(userProfile.get());
                userGroupMembership.setTenantId(tenantId);

                userGroupMembership.setUserGroupMembershipType(exist);
                groupMembershipRepository.save(userGroupMembership);

                return GroupMapper.createGroup(exOP.get(), userGroupMembership.getUserProfile().getUsername());
            } else {

                String msg = "Error occurred while creating Group for " + request.getTenantId()
                        + " reason : DB error";
                LOGGER.error(msg);
                throw new RuntimeException(msg);
            }


        } catch (Exception ex) {
            String msg = "Error occurred while creating Group for " + request.getTenantId() + " reason :" + ex.getMessage();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public com.veda.central.core.user.profile.api.Group updateGroup(GroupRequest request) {
        try {
            LOGGER.debug("Request received to updateGroup for group with id  " + request.getGroup().getId() + " at tenant " + request.getTenantId());

            String groupId = request.getGroup().getId();
            long tenantId = request.getTenantId();
            String effectiveId = groupId + "@" + tenantId;

            Optional<Group> exEntity = groupRepository.findById(effectiveId);

            if (exEntity.isPresent()) {
                Group entity = GroupMapper.createGroupEntity(request.getGroup(), request.getTenantId());
                entity.setCreatedAt(exEntity.get().getCreatedAt());
                Group exGroup = exEntity.get();

                if (exGroup.getGroupAttribute() != null) {
                    groupAttributeRepository.deleteAll(exGroup.getGroupAttribute());
                }

                if (exGroup.getGroupRole() != null) {
                    groupRoleRepository.deleteAll(exGroup.getGroupRole());
                }

                groupRepository.save(entity);
                Optional<Group> exOP = groupRepository.findById(effectiveId);

                if (exOP.isPresent()) {
                    List<UserGroupMembership> userGroupMemberships = groupMembershipRepository.findAllByGroupId(effectiveId);

                    String ownerId = null;
                    for (UserGroupMembership userGroupMembership : userGroupMemberships) {
                        if (userGroupMembership.getUserGroupMembershipType().getId().equals(DefaultGroupMembershipTypes.OWNER.name())) {
                            ownerId = userGroupMembership.getUserProfile().getUsername();
                        }
                    }

                    return GroupMapper.createGroup(exOP.get(), ownerId);

                } else {
                    String msg = "Error occurred while updating group  " + request.getTenantId() + " reason : DB error";
                    LOGGER.error(msg);
                    throw new RuntimeException(msg);
                }

            } else {
                String msg = "Cannot find a group for " + groupId;
                LOGGER.error(msg);
                throw new EntityNotFoundException(msg);
            }

        } catch (Exception ex) {
            String msg = "Error occurred while updating group " + request.getGroup().getId() + "at " + request.getTenantId() + " reason :" + ex.getMessage();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public void deleteGroup(GroupRequest request) {
        try {
            LOGGER.debug("Request received to deleteGroup for " + request.getGroup().getId() + "at " + request.getTenantId());

            String userId = request.getGroup().getId();
            long tenantId = request.getTenantId();

            String effectiveId = userId + "@" + tenantId;

            Optional<Group> op = groupRepository.findById(effectiveId);

            if (op.isPresent()) {
                Group entity = op.get();

                List<UserGroupMembership> userGroupMemberships = groupMembershipRepository.findAllByGroupId(effectiveId);

                String ownerId = userGroupMemberships.stream()
                        .filter(u -> u.getUserGroupMembershipType().getId().equals(DefaultGroupMembershipTypes.OWNER.name()))
                        .map(u -> u.getUserProfile().getUsername())
                        .findAny()
                        .orElse(null);

                com.veda.central.core.user.profile.api.Group prof = GroupMapper.createGroup(entity, ownerId);

                groupRepository.delete(op.get());

                List<Group> groupList = groupRepository.findByParentId(entity.getId());

                if (groupList != null && !groupList.isEmpty()) {
                    groupRepository.deleteAll(groupList);

                }

            } else {
                String msg = "Cannot find a group for " + request.getId();
                LOGGER.error(msg);
                throw new EntityNotFoundException(msg);
            }

        } catch (Exception ex) {
            String msg = "Error occurred while deleting group for " + request.getGroup() + "at " + request.getTenantId() + " reason :" + ex.getMessage();
            LOGGER.error(msg, ex);
        }
    }

    public com.veda.central.core.user.profile.api.Group getGroup(GroupRequest request) {
        try {
            LOGGER.debug("Request received to getGroup for group " + request.getGroup().getId() + "at " + request.getTenantId());

            String userId = request.getGroup().getId();
            long tenantId = request.getTenantId();
            String effectiveId = userId + "@" + tenantId;
            Optional<Group> op = groupRepository.findById(effectiveId);

            if (op.isPresent()) {
                List<UserGroupMembership> userGroupMemberships = groupMembershipRepository.findAllByGroupId(effectiveId);
                String ownerId = userGroupMemberships.stream()
                        .filter(userGroupMembership -> userGroupMembership.getUserGroupMembershipType().getId().equals(DefaultGroupMembershipTypes.OWNER.name()))
                        .map(userGroupMembership -> userGroupMembership.getUserProfile().getUsername())
                        .findFirst()
                        .orElse(null);

                return GroupMapper.createGroup(op.get(), ownerId);

            } else {
                throw new EntityNotFoundException("Could not find the Group with the Id: " + userId);
            }

        } catch (Exception ex) {
            String msg = "Error occurred while fetching group " + request.getGroup().getId() + "at " + request.getTenantId() + " reason :" + ex.getMessage();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public GetAllGroupsResponse getAllGroups(GroupRequest request) {
        try {
            LOGGER.debug("Request received to getAllGroups for " + request.getTenantId());

            List<Group> groups = groupRepository.searchEntities(request.getTenantId(), request.getGroup(), request.getOffset(), request.getLimit());
            if (groups == null || groups.isEmpty()) {
                groups = groupRepository.findAllByTenantId(request.getTenantId());
            }

            List<com.veda.central.core.user.profile.api.Group> groupList = new ArrayList<>();
            if (groups != null && !groups.isEmpty()) {

                for (Group group : groups) {
                    List<UserGroupMembership> userGroupMemberships = groupMembershipRepository.findAllByGroupId(group.getId());
                    String ownerId = null;

                    Optional<UserGroupMembership> ownerOptional = userGroupMemberships.stream()
                            .filter(userGroupMembership -> userGroupMembership
                                    .getUserGroupMembershipType()
                                    .getId()
                                    .equals(DefaultGroupMembershipTypes.OWNER.name()))
                            .findFirst();

                    if (ownerOptional.isPresent()) {
                        ownerId = ownerOptional.get().getUserProfile().getUsername();
                    }

                    com.veda.central.core.user.profile.api.Group gr = GroupMapper.createGroup(group, ownerId);
                    groupList.add(gr);
                }
            }

            return GetAllGroupsResponse.newBuilder().addAllGroups(groupList).build();

        } catch (Exception ex) {
            String msg = "Error occurred while fetching groups of client " + request.getClientId() + " reason :" + ex;
            LOGGER.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    public Status addUserToGroup(GroupMembership request) {
        try {
            LOGGER.debug("Request received to addUserToGroup for " + request.getTenantId());

            String group_id = request.getGroupId();
            String username = request.getUsername();
            long tenantId = request.getTenantId();
            String userId = username + "@" + tenantId;
            String effectiveGroupId = group_id + "@" + tenantId;
            Optional<Group> group = groupRepository.findById(effectiveGroupId);
            Optional<UserProfile> userProfile = repository.findById(userId);

            if (group.isPresent() && userProfile.isPresent()) {

                List<UserGroupMembership> memberships = groupMembershipRepository.findAllByGroupIdAndUserProfileId(effectiveGroupId, userId);

                if (memberships == null || memberships.isEmpty()) {
                    String type = request.getType();
                    type = type.isBlank() ? DefaultGroupMembershipTypes.MEMBER.name() : type.toUpperCase();

                    Optional<UserGroupMembershipType> groupMembershipType = groupMembershipTypeRepository.findById(type);
                    UserGroupMembershipType exist;

                    if (groupMembershipType.isEmpty()) {
                        exist = new UserGroupMembershipType();
                        exist.setId(type);
                        groupMembershipTypeRepository.save(exist);
                    } else {
                        exist = groupMembershipType.get();
                    }

                    UserGroupMembership userGroupMembership = new UserGroupMembership();
                    userGroupMembership.setGroup(group.get());
                    userGroupMembership.setUserProfile(userProfile.get());
                    userGroupMembership.setTenantId(tenantId);
                    userGroupMembership.setUserGroupMembershipType(exist);
                    groupMembershipRepository.save(userGroupMembership);
                }

                return Status.newBuilder().setStatus(true).build();

            } else {
                String msg = "Group or user not available";
                LOGGER.error(msg);
                throw new EntityNotFoundException("Group or user not available");
            }

        } catch (Exception ex) {
            String msg = "Error occurred while add user to  group at  " + request.getTenantId() + " reason :" + ex;
            LOGGER.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    public Status removeUserFromGroup(GroupMembership request) {
        try {
            LOGGER.debug("Request received to removeUserFromGroup for " + request.getTenantId());

            String group_id = request.getGroupId();
            String username = request.getUsername();
            long tenantId = request.getTenantId();
            String userId = username + "@" + tenantId;
            String effectiveGroupId = group_id + "@" + tenantId;

            List<UserGroupMembership> memberships = groupMembershipRepository.findAllByGroupIdAndUserProfileId(effectiveGroupId, userId);
            List<UserGroupMembership> userGroupMemberships = groupMembershipRepository.findAllByGroupIdAndUserGroupMembershipTypeId(effectiveGroupId, DefaultGroupMembershipTypes.OWNER.name());

            if (userGroupMemberships != null && userGroupMemberships.size() == 1 && userGroupMemberships.get(0).getUserProfile().getUsername().equals(username)) {
                String msg = "Default owner " + username + " cannot be removed from group " + group_id;
                LOGGER.error(msg);
                throw new IllegalArgumentException(msg);
            }

            if (memberships != null && !memberships.isEmpty()) {
                groupMembershipRepository.deleteAll(memberships);
            }

            return Status.newBuilder().setStatus(true).build();

        } catch (Exception ex) {
            String msg = "Error occurred while removing user from  in client " + request.getClientId() + " reason :" + ex;
            LOGGER.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    public GetAllGroupsResponse getAllGroupsOfUser(UserProfileRequest request) {
        try {
            LOGGER.debug("Request received to getAllGroupsOfUser for " + request.getTenantId());

            long tenantId = request.getTenantId();
            String username = request.getProfile().getUsername();
            String userId = username + "@" + tenantId;

            List<UserGroupMembership> userGroupMemberships = groupMembershipRepository.findAllByUserProfileId(userId);
            List<Group> groups = new ArrayList<>();

            if (userGroupMemberships != null && !userGroupMemberships.isEmpty()) {
                userGroupMemberships.forEach(userGroupMembership -> {
                    AtomicBoolean toBeAdded = new AtomicBoolean(true);
                    groups.forEach(le -> {
                        if (le.getId().equals(userGroupMembership.getGroup().getId())) {
                            toBeAdded.set(false);
                        }
                    });

                    if (toBeAdded.get()) {
                        groups.add(userGroupMembership.getGroup());
                    }
                });

            }

            Map<String, Group> groupMap = getAllUniqueGroups(groups, null);
            List<com.veda.central.core.user.profile.api.Group> groupList = new ArrayList<>();

            groupMap.keySet().forEach(gr -> {
                List<UserGroupMembership> memberships = groupMembershipRepository.findAllByGroupId(gr);

                String ownerId = null;
                for (UserGroupMembership userGroupMembership : memberships) {
                    if (userGroupMembership.getUserGroupMembershipType().getId().equals(DefaultGroupMembershipTypes.OWNER.name())) {
                        ownerId = userGroupMembership.getUserProfile().getUsername();
                        break;
                    }
                }
                groupList.add(GroupMapper.createGroup(groupMap.get(gr), ownerId));
            });

            return GetAllGroupsResponse.newBuilder().addAllGroups(groupList).build();

        } catch (Exception ex) {
            String msg = "Error occurred while fetching user groups of user " + request.getProfile().getUsername() + " in tenant " + request.getTenantId() + " reason :" + ex;
            LOGGER.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    public Status addUserGroupMembershipType(UserGroupMembershipTypeRequest request) {
        try {
            LOGGER.debug("Request received to addUserGroupMembershipType of type  " + request.getType());

            String type = request.getType().toUpperCase();
            Optional<UserGroupMembershipType> userGroupMembershipType = groupMembershipTypeRepository.findById(type);

            if (userGroupMembershipType.isEmpty()) {
                UserGroupMembershipType userGroupType = new UserGroupMembershipType();
                userGroupType.setId(type);
                groupMembershipTypeRepository.save(userGroupType);
            }

            return Status.newBuilder().setStatus(true).build();

        } catch (Exception ex) {
            String msg = "Error occurred while saving group membership type" + request.getType();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public Status removeUserGroupMembershipType(UserGroupMembershipTypeRequest request) {
        try {
            LOGGER.debug("Request received to removeUserGroupMembershipType of type " + request.getType());

            String type = request.getType().toUpperCase();
            Optional<UserGroupMembershipType> userGroupMembershipType = groupMembershipTypeRepository.findById(type);
            userGroupMembershipType.ifPresent(groupMembershipType -> groupMembershipTypeRepository.delete(groupMembershipType));
            return Status.newBuilder().setStatus(true).build();

        } catch (Exception ex) {
            String msg = "Error occurred while deleting removeUserGroupMembershipType of type  " + request.getType();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    public Status changeUserMembershipType(GroupMembership request) {
        try {
            LOGGER.debug("Request received to changeUserMembershipType in   tenant" + request.getTenantId() + " with id " + request.getUsername() + " to " + request.getType());

            long tenantId = request.getTenantId();
            String username = request.getUsername();
            String groupId = request.getGroupId();
            String type = request.getType();
            String userId = username + "@" + tenantId;
            String effectiveGroupId = groupId + "@" + tenantId;

            List<UserGroupMembership> userGroupMemberships = groupMembershipRepository.findAllByGroupIdAndUserProfileId(effectiveGroupId, userId);

            if (userGroupMemberships == null || userGroupMemberships.isEmpty()) {
                String msg = "group membership not found";
                LOGGER.error(msg);
                throw new EntityNotFoundException(msg);
            }

            List<UserGroupMembership> userMemberships = groupMembershipRepository
                    .findAllByGroupIdAndUserGroupMembershipTypeId(effectiveGroupId, DefaultGroupMembershipTypes.OWNER.name());

            if (userMemberships != null && userMemberships.size() == 1 && userMemberships.get(0).getUserProfile().getUsername().equals(username)) {
                String msg = "Default owner " + username + " cannot be changed for group " + groupId;
                LOGGER.error(msg);
                throw new IllegalArgumentException(msg);
            }

            UserGroupMembership groupMembership = userGroupMemberships.get(0);
            UserGroupMembershipType groupMembershipType = groupMembership.getUserGroupMembershipType();
            groupMembershipType.setId(type);

            groupMembership.setUserGroupMembershipType(groupMembershipType);
            groupMembershipRepository.save(groupMembership);

            if (type.equals(DefaultGroupMembershipTypes.OWNER.name())) {

                List<UserGroupMembership> memberships = groupMembershipRepository.findAllByGroupId(effectiveGroupId);

                if (memberships != null && !memberships.isEmpty()) {
                    for (UserGroupMembership membership : memberships) {
                        if (membership.getUserGroupMembershipType().getId().equals(DefaultGroupMembershipTypes.OWNER.name())
                                && !membership.getUserProfile().getUsername().equals(userId)) {

                            groupMembershipRepository.delete(membership);

                            UserGroupMembership userGroupMembership = new UserGroupMembership();
                            userGroupMembership.setGroup(membership.getGroup());
                            userGroupMembership.setUserProfile(membership.getUserProfile());
                            userGroupMembership.setTenantId(tenantId);

                            Optional<UserGroupMembershipType> membershipType = groupMembershipTypeRepository.findById(DefaultGroupMembershipTypes.MEMBER.name());
                            userGroupMembership.setUserGroupMembershipType(membershipType.get());
                            groupMembershipRepository.save(userGroupMembership);
                        }
                    }
                }
            }
            return Status.newBuilder().setStatus(true).build();

        } catch (Exception ex) {
            String msg = "Error occurred while changing membership type  in   tenant" + request.getTenantId() + " with id " + request.getUsername() + " to " + request.getType();
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public Status hasAccess(GroupMembership request) {
        try {
            LOGGER.debug("Request received to check access  in   tenant"
                    + request.getTenantId() + " with id " + request.getUsername() + " to " + request.getType());

            long tenantId = request.getTenantId();
            String username = request.getUsername();
            String groupId = request.getGroupId();
            String type = request.getType();
            String userId = username + "@" + tenantId;
            String effectiveGroupId = groupId + "@" + tenantId;

            List<UserGroupMembership> userGroupMemberships = groupMembershipRepository.findAllByGroupIdAndUserProfileIdAndUserGroupMembershipTypeId(effectiveGroupId, userId, type);
            return Status.newBuilder().setStatus(userGroupMemberships != null && !userGroupMemberships.isEmpty()).build();

        } catch (Exception ex) {
            String msg = "Error occurred while checking access to   in   tenant" + request.getTenantId() + " with id " + request.getUsername() + " to " + request.getType();
            LOGGER.error(msg);
            throw new RuntimeException(msg, ex);
        }
    }

    private Map<String, Group> getAllUniqueGroups(List<Group> leaveGroups, Map<String, Group> allParentGroups) {
        if (allParentGroups == null) {
            allParentGroups = new HashMap<>();
        }

        if (leaveGroups != null && !leaveGroups.isEmpty()) {
            for (Group gr : leaveGroups) {
                List<GroupToGroupMembership> memberships = groupToGroupMembershipRepository.findAllByChildId(gr.getId());
                List<Group> leaves = new ArrayList<>();
                allParentGroups.put(gr.getId(), gr);

                if (memberships != null && !memberships.isEmpty()) {
                    memberships.forEach(mem -> {
                        AtomicBoolean toBeAdded = new AtomicBoolean(true);
                        leaves.forEach(le -> {
                            if (le.getId().equals(mem.getParent().getId())) {
                                toBeAdded.set(false);
                            }
                        });

                        if (toBeAdded.get()) {
                            leaves.add(mem.getParent());
                        }
                    });

                    getAllUniqueGroups(leaves, allParentGroups);
                }
            }
        }

        return allParentGroups;
    }

}
