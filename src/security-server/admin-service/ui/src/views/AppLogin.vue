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
  <v-container fluid fill-height class="login-view-wrap">
    <alerts-container class="alerts" />
    <div class="graphics">
      <v-img
        :src="require('../assets/xroad7_large.svg')"
        height="195"
        width="144"
        max-height="195"
        max-width="144"
        class="xrd-logo"
      ></v-img>
    </div>

    <v-layout align-center justify-center>
      <v-flex class="set-width">
        <v-card flat>
          <v-toolbar flat class="login-form-toolbar">
            <div class="title-wrap">
              <div class="login-form-toolbar-title">
                {{ $t('login.logIn') }}
              </div>
              <div class="sub-title">X-Road Security Server</div>
            </div>
          </v-toolbar>

          <v-card-text>
            <v-form>
              <ValidationObserver ref="form">
                <ValidationProvider
                  v-slot="{ errors }"
                  name="username"
                  rules="required"
                >
                  <v-text-field
                    id="username"
                    v-model="username"
                    name="username"
                    outlined
                    :label="$t('fields.username')"
                    :error-messages="errors"
                    type="text"
                    autofocus
                    @keyup.enter="submit"
                  ></v-text-field>
                </ValidationProvider>

                <ValidationProvider
                  v-slot="{ errors }"
                  name="password"
                  rules="required"
                >
                  <v-text-field
                    id="password"
                    v-model="password"
                    name="password"
                    outlined
                    :label="$t('fields.password')"
                    :error-messages="errors"
                    type="password"
                    @keyup.enter="submit"
                  ></v-text-field>
                </ValidationProvider>
              </ValidationObserver>
            </v-form>
          </v-card-text>
          <v-card-actions class="px-4">
            <xrd-button
              id="submit-button"
              color="primary"
              gradient
              block
              large
              :min_width="120"
              rounded
              :disabled="isDisabled"
              :loading="loading"
              @click="submit"
              >{{ $t('login.logIn') }}
            </xrd-button>
          </v-card-actions>
        </v-card>
      </v-flex>
    </v-layout>
  </v-container>
</template>

<script lang="ts">
import Vue, { VueConstructor } from 'vue';
import { Permissions, RouteName } from '@/global';
import { ValidationObserver, ValidationProvider } from 'vee-validate';
import AlertsContainer from '@/components/ui/AlertsContainer.vue';
import axios, { AxiosError } from 'axios';
import { mapActions, mapState } from 'pinia';
import { useUser } from '@/store/modules/user';
import { useSystem } from '@/store/modules/system';
import { useNotifications } from '@/store/modules/notifications';

export default (
  Vue as VueConstructor<
    Vue & {
      $refs: {
        form: InstanceType<typeof ValidationObserver>;
      };
    }
  >
).extend({
  name: 'Login',
  components: {
    ValidationProvider,
    ValidationObserver,
    AlertsContainer,
  },
  data() {
    return {
      loading: false as boolean,
      username: '' as string,
      password: '' as string,
    };
  },
  computed: {
    ...mapState(useUser, [
      'hasPermission',
      'firstAllowedTab',
      'hasInitState',
      'needsInitialization',
    ]),
    isDisabled() {
      if (
        this.username.length < 1 ||
        this.password.length < 1 ||
        this.loading
      ) {
        return true;
      }
      return false;
    },
  },
  methods: {
    ...mapActions(useUser, [
      'loginUser',
      'logoutUser',
      'fetchInitializationStatus',
      'fetchUserData',
      'fetchCurrentSecurityServer',
      'clearAuth',
    ]),
    ...mapActions(useSystem, [
      'fetchSecurityServerVersion',
      'fetchSecurityServerNodeType',
      'clearSystemStore',
    ]),
    ...mapActions(useNotifications, [
      'showError',
      'showErrorMessage',
      'clearErrorNotifications',
    ]),
    async submit() {
      // Clear error notifications when route is changed
      this.clearErrorNotifications();

      /* Clear user data so there is nothing left from previous sessions.
       For example user has closed browser tab without loggin out > user data is left in browser local storage */
      this.clearAuth();
      this.clearSystemStore();

      // Validate inputs
      const isValid = await this.$refs.form.validate();

      if (!isValid) return;

      const loginData = {
        username: this.username,
        password: this.password,
      };

      this.$refs.form.reset();
      this.loading = true;

      try {
        await this.loginUser(loginData);
      } catch (error) {
        if (axios.isAxiosError(error)) {
          // Display invalid username/password error in inputs
          if (error?.response?.status === 401) {
            // Clear inputs
            this.username = '';
            this.password = '';
            this.$refs.form.reset();

            // The whole view needs to be rendered so the "required" rule doesn't block
            // "wrong unsername or password" error in inputs
            this.$nextTick(() => {
              // Set inputs to error state
              this.$refs.form.setErrors({
                username: [''],
                password: [this.$t('login.errorMsg401') as string],
              });
            });
          }
          this.showErrorMessage(this.$t('login.generalError'));
        } else {
          if (error instanceof Error) {
            this.showErrorMessage(error.message);
          } else {
            throw error;
          }
        }
      }

      // Auth ok. Start phase 2 (fetch user data and current security server info).

      try {
        await this.fetchUserData();
        await this.fetchInitializationData(); // Used to be inside fetchUserData()
        await this.fetchSecurityServerVersion();
        await this.fetchSecurityServerNodeType();
      } catch (error) {
        this.showError(error as AxiosError);
      }

      // Clear loading state
      this.loading = false;
    },

    async fetchInitializationData() {
      const redirectToLogin = async () => {
        // Logout without page refresh
        await this.logoutUser(false);
        // Clear inputs
        this.username = '';
        this.password = '';
        this.$refs.form.reset();
      };

      await this.fetchInitializationStatus();
      await this.fetchSecurityServerNodeType();
      if (!this.hasInitState) {
        this.showErrorMessage(
          this.$t('initialConfiguration.noInitializationStatus'),
        );
        await redirectToLogin();
      } else if (this.needsInitialization) {
        // Check if the user has permission to initialize the server
        if (!this.hasPermission(Permissions.INIT_CONFIG)) {
          await redirectToLogin();
          throw new Error(
            this.$t('initialConfiguration.noPermission') as string,
          );
        }
        await this.$router.replace({ name: RouteName.InitialConfiguration });
      } else {
        // No need to initialise, proceed to "main view"
        await this.fetchCurrentSecurityServer();
        await this.$router.replace({
          name: this.firstAllowedTab.to.name,
        });
      }
    },
  },
});
</script>

<style lang="scss" scoped>
@import '~styles/colors';

.alerts {
  top: 40px;
  left: 0;
  right: 0;
  margin-left: auto;
  margin-right: auto;
  z-index: 100;
  position: absolute;
}

.graphics {
  height: 100%;
  width: 40%;
  max-width: 576px; // width of the backround image
  background-image: url('../assets/background.png');
  background-size: cover;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.login-view-wrap {
  background-color: white;
  padding: 0;
}

.title-wrap {
  display: flex;
  flex-direction: column;
}

.login-form-toolbar {
  background-color: white;
  margin-bottom: 30px;
  padding-left: 0;
}

.login-form-toolbar-title {
  margin-left: 0;
  color: #252121;
  font-style: normal;
  font-weight: bold;
  font-size: 40px;
  line-height: 54px;
}

.sub-title {
  font-style: normal;
  font-weight: normal;
  font-size: $XRoad-DefaultFontSize;
  line-height: 19px;
}

.set-width {
  max-width: 420px;
}
</style>
