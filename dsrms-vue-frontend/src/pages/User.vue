<template>
    <div v-loading.fullscreen.lock="fullscreenLoading">
      <br/>
      <div id="header">
        <el-page-header @back="goBack" v-bind:title="(myRole)" v-bind:content="myName + ' - ' + mySchool"></el-page-header>
        <el-tabs v-model="activeName">
          <el-tab-pane label="" name="">
          </el-tab-pane>
          <el-tab-pane label="Course" name="first">
            <Course
                v-bind:course="courseArray" v-bind:role="myRole"
                v-bind:base-url="baseUrl" v-bind:peers="peers"
            />
          </el-tab-pane>
          <el-tab-pane label="Quiz" name="second">
            <Quiz
                v-bind:course="courseArray" v-bind:role="myRole"
                v-bind:base-url="baseUrl" v-bind:peers="peers" v-bind:quiz="quizArray"
            />
          </el-tab-pane>
          <el-tab-pane label="Activity" name="third">
            <Activity v-bind:base-url="baseUrl" v-bind:my-party="myParty" v-bind:activities="activityArray" />
          </el-tab-pane>
          <el-tab-pane label="People" name="fourth">
            <People v-bind:base-url="baseUrl" v-bind:role="myRole" v-bind:me="myName"
                    v-bind:peers="peers" v-bind:conductRecord="conductArray" v-bind:aboutMe="aboutMe"
            />
          </el-tab-pane>
        </el-tabs>
      </div>
      <div id="main"></div>

    </div>
</template>

<script>
    import Course from '../components/Course.vue';
    import People from "../components/People";
    import Quiz from "../components/Quiz";
    import Activity from "../components/Activity";

    export default {
        name: 'User',
        props: {
            msg: String
        },
        data() {
            return {
                port: '',
                peers: [],
                activeName: 'first',
                fullscreenLoading: false,
                myName: '',
                myRole: '',
                myParty: '',
                mySchool: '',
                myCountry: '',
                baseUrl: '',
                courseArray: [],
                quizArray: [],
                doQuizArray: [],
                gradeArray: [],
                activityArray: [],
                attendanceArray: [],
                certificationArray: [],
                conductArray: [],
                aboutMe: [],
            }
        },
        methods: {
            getMyRole() {
                this.$axios.get(`${this.baseUrl}/me`).then(res => {
                    let data = res.data;
                    // console.log(data);
                    let myIDs = data['me'].split(', ');
                    this.myName = myIDs[0].split('=')[1];
                    this.myRole = myIDs[1].split('=')[1];
                    this.myParty = myIDs[2].split('=')[1];
                    this.mySchool = myIDs[3].split('=')[1];
                    this.myCountry = myIDs[4].split('=')[1];
                    let meInfoArray = [];
                    let meInfoDict = {}
                    meInfoDict["myName"] = this.myName;
                    meInfoDict["myRole"] = this.myRole;
                    meInfoDict["myParty"] = this.myParty;
                    meInfoDict["mySchool"] = this.mySchool;
                    meInfoDict["myCountry"] = this.myCountry;
                    meInfoArray.push(meInfoDict);
                    this.aboutMe = meInfoArray;
                });
            },
            getPeers() {
                this.$axios.get(`${this.baseUrl}/peers`).then(res => {
                    let data = res.data.peers;
                    let peersArray = [];
                    data.forEach(function (item) {
                        let obj = {}
                        item.split(', ').forEach(function (element) {
                            let key = element.split('=')[0];
                            obj[key] = element.split('=')[1];
                        })
                        peersArray.push(obj)
                    })
                    this.peers = peersArray
                    // console.log(this.peers);
                });
            },
            getMyRecords() {
                this.$axios.get(`${this.baseUrl}/my-records`).then(res => {
                    let data = res.data;
                    this.courseArray = data.Course;
                    this.quizArray = data.Quiz;
                    this.doQuizArray = data.DoQuiz;
                    this.gradeArray = data.Grade;
                    this.activityArray = data.Activity;
                    this.attendanceArray = data.Attendance;
                    this.certificationArray = data.Certification;
                    this.conductArray = data.Conduct;
                    // console.log(this.conductArray)
                });
            },
            goBack() {
                window.history.length > 1 ? this.$router.go(-1) : this.$router.push('/')
            },
        },
        created() {
            this.port = this.$route.params.port;
            this.baseUrl = `http://localhost:${this.port}/DSRMS/api`;
            this.getPeers();

            this.fullscreenLoading = true;
            setTimeout(() => {
                this.fullscreenLoading = false;
            }, 900);

            this.getMyRole();

        },
        components: {
            Course,
            People,
            Quiz,
            Activity,
        },
        mounted() {
            this.getMyRecords();
        }
    }
</script>

<style>
  #main {
    margin-left: 2%;
    margin-right: 2%;
  }

  /*This larger the font size of tab bar*/
  .el-tabs__item {
    font-size: larger!important;
  }
</style>
