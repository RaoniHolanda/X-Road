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
  <v-container
    v-if="isAuthenticated && isServerInitialized && hasAlerts"
    fluid
    class="alerts-container"
  >
    <v-alert
      v-if="alerts"
      :value="true"
      color="red"
      border="left"
      colored-border
      class="alert"
      icon="icon-Error-notification"
    >
      <span v-for="item in alerts" :key="item.errorCode" class="alert-text">
        {{ $t(item.errorCode, reformatDates(item.metadata)) }}
      </span>
    </v-alert>

    <v-alert
      data-test="global-alert-restore"
      :value="showRestoreInProgress"
      color="red"
      border="left"
      colored-border
      class="alert"
      icon="icon-Error-notification"
    >
      <span class="alert-text">{{
        $t('globalAlert.backupRestoreInProgress', {
          startTime: formatDateTime(restoreStartTime),
        })
      }}</span>
    </v-alert>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue';
import { formatDateTime, formatDateTimeSeconds } from '@/filters';
import { mapState } from 'pinia';
import { useAlerts } from '@/store/modules/alerts';
import { useUser } from '@/store/modules/user';
import { useSystem } from '@/store/modules/system';

export default Vue.extend({
  name: 'AlertsContainer',
  computed: {
    ...mapState(useAlerts, ['alerts']),
    ...mapState(useUser, ['isAuthenticated']),
    ...mapState(useSystem, ['isServerInitialized']),
    hasAlerts(): boolean {
      return this.showRestoreInProgress || this.alerts?.length > 0;
    },
    showRestoreInProgress(): boolean {
      // Mock. Add vuex getter later.
      return false;
    },
    restoreStartTime(): boolean {
      // Mock. Add vuex getter later.
      return true;
    },
  },
  methods: {
    formatDateTime,
    reformatDates(metadata: string[]): string[] {
      return metadata.map((item) => {
        if (isNaN(Date.parse(item))) {
          return item;
        }
        return formatDateTimeSeconds(item);
      });
    },
  },
});
</script>

<style scoped lang="scss">
@import '~styles/colors';

.alerts-container {
  padding: 0;

  & > * {
    margin-bottom: 4px;
    border-radius: 0;
  }

  & > :first-child {
    margin-top: 16px;
  }
}

.alert {
  margin-top: 16px;
  border: 2px solid $XRoad-WarmGrey30;
  box-sizing: border-box;
  border-radius: 4px;
}

.alert-text {
  color: $XRoad-Black100;
  display: block;
}

.clickable-link {
  text-decoration: underline;
  cursor: pointer;
}
</style>
