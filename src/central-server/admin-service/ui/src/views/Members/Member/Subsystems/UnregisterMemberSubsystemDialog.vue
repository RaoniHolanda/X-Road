<!--
   The MIT License

   Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
   Copyright (c) 2018 Estonian Information System Authority (RIA),
   Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
   Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
   THE SOFTWARE.
 -->
<template>
  <xrd-sub-view-container>
    <v-dialog v-if="true" :value="true" width="500" persistent>
      <v-card class="xrd-card">
        <v-card-title>
          <span class="headline">
            {{ $t('members.member.subsystems.deleteSubsystem') }}
          </span>
        </v-card-title>
        <v-card-text class="pt-4" data-test="unregister-subsystem">
          <i18n path="members.member.subsystems.areYouSureUnregister">
            <template #subsystemCode>
              <b>{{ subsystemCode }}</b>
            </template>
            <template #serverCode>
              <b>{{ serverCode }}</b>
            </template>
          </i18n>
        </v-card-text>
        <v-card-actions class="xrd-card-actions">
          <v-spacer></v-spacer>
          <xrd-button
            outlined
            :disabled="loading"
            data-test="dialog-cancel-button"
            @click="cancel()"
          >
            {{ $t('action.cancel') }}
          </xrd-button>
          <xrd-button
            data-test="dialog-unregister-button"
            :disabled="loading"
            @click="unregisterSubsystem()"
          >
            {{ $t('action.delete') }}
          </xrd-button>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </xrd-sub-view-container>
</template>

<script lang="ts">
import Vue from 'vue';
import { mapActions, mapState, mapStores } from 'pinia';
import { useClient } from '@/store/modules/clients';
import { useSystem } from '@/store/modules/system';
import { useNotifications } from '@/store/modules/notifications';
import { useSubsystem } from '@/store/modules/subsystems';
import { useMember } from '@/store/modules/members';
import { toIdentifier } from '@/util/helpers';
import { Client } from '@/openapi-types';

export default Vue.extend({
  name: 'UnregisterMemberSubsystemDialog',
  props: {
    showDialog: {
      type: Boolean,
      required: true,
    },
    subsystemCode: {
      type: String,
      required: true,
    },
    serverCode: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      loading: false,
    };
  },
  computed: {
    ...mapStores(useClient, useMember, useSubsystem),
    ...mapState(useSystem, ['getSystemStatus']),
  },
  methods: {
    ...mapActions(useNotifications, ['showError', 'showSuccess']),
    cancel(): void {
      this.$emit('cancel');
    },
    unregisterSubsystem(): void {
      this.loading = true;
      const currentMember = this.memberStore.$state.currentMember;
      this.subsystemStore
        .unregisterById(
          toIdentifier(currentMember.client_id) + ':' + this.subsystemCode,
          toIdentifier(currentMember.client_id) + ':' + this.serverCode,
        )
        .then(() => {
          this.showSuccess(
            this.$t(
              'members.member.subsystems.subsystemSuccessfullyUnregistered',
              {
                subsystemCode: this.subsystemCode,
                serverCode: this.serverCode,
              },
            ),
          );
          this.$emit('unregisteredSubsystem');
        })
        .catch((error) => {
          this.showError(error);
          this.$emit('cancel');
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
});
</script>
