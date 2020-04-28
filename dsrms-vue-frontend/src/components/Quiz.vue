<template>
  <div class="main">
    <div v-if="role === 'Teacher'" class="create">
      <el-divider content-position="left" id="create-quiz">Create Quiz</el-divider>
      <el-card class="box-card" shadow="hover">
        <div slot="header" class="clearfix">
          <span>Create a quiz</span>
          <el-tooltip content="Create a quiz" placement="right">
            <el-button style="float: right; padding: 3px 0; font-size: large;" type="primary" plain @click="createQuiz">
              Create!
            </el-button>
          </el-tooltip>

        </div>

        <el-select v-model="selectedCourse" clearable placeholder="Select a course" @click.native="groupCourses">
          <el-option
              v-for="item in courseCategories"
              :key="item"
              :label="item"
              :value="item">
          </el-option>
        </el-select>

        <br/><br/>
        <el-input
            placeholder="Quiz Name"
            v-model="eventValue"
            clearable
            @change="handleEventValueChange"
        >
        </el-input>
        <br/><br/>
        <el-input
            type="textarea"
            :rows="5"
            placeholder="Quiz Questions"
            v-model="eventDescription"
            @change="handleEventDesChange"
        >
        </el-input>
        <br/><br/>
        <el-upload
            class="upload-demo"
            drag
            action="https://jsonplaceholder.typicode.com/posts/"
            multiple>
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">Drop file here, or <em>click to upload</em></div>
          <div class="el-upload__tip" slot="tip">The file size should smaller than 5MB</div>
        </el-upload>
        <br/>
      </el-card>
    </div>

    <br/>
    <el-divider content-position="left">Quiz List</el-divider>
    <el-card class="box-card" shadow="hover" >
      <el-collapse v-model="activeNames">
        <el-collapse-item :key="item.ref.txhash" v-for="(item,index) in quiz" :title="item.state.data.eventValue" :name="index">
          <el-card class="box-card" shadow="hover">
            <div slot="header" class="clearfix" >
              <span style="float: left;">Course details</span>
              <div class="teacher-btn-gp" v-if="role === 'Teacher'">
                <el-button style="float: right; padding: 1px 0" type="text">Modify</el-button>
                <!--                <el-button style="float: right; padding: 1px 0" type="text">Create Quiz | </el-button>-->
              </div>

            </div>
            <div class="text-item">
              <p>Teacher: {{ item.state.data.issuer.split(', ')[0].split('=')[1] }}</p>
              <p>Student: {{ item.state.data.receiver.split(', ')[0].split('=')[1] }}</p>
              <p>Course Description: {{ item.state.data.eventDescriptions }}</p>
            </div>
          </el-card>
        </el-collapse-item>
      </el-collapse>
    </el-card>


  </div>
</template>

<script>
    export default {
        name: "Quiz",
        props: {
            role: String,
            course: Array,
            peers: Array,
            baseUrl: String,
            quiz: Array,
        },
        data() {
            return {
                groupedCourses: {},
                courseCategories: [],
                eventDescription: '',
                eventValue: '',
                selectedCourse: '',
            }
        },
        methods: {
            groupCourses() {
                let allCourses = this.course;
                let groupedCourses = {};
                let categories = [];
                // Get unique course name
                allCourses.forEach(function(item) {
                    let itemName = item.state.data.eventValue;
                    if (!categories.includes(itemName)) {
                        categories.push(itemName);
                    }
                });
                // Initial a key-value pair object
                categories.forEach(function(courseName) {
                    groupedCourses[courseName] = [];
                });
                // Push course object in to the key-value pair object
                for (let key in categories) {
                    // let belongsToCurrentKey = [];

                    allCourses.forEach(function(item) {
                        let itemName = item.state.data.eventValue;
                        let itemArray = item.state.data;
                        if (itemName === categories[key]) {
                            groupedCourses[itemName].push(itemArray);

                        }
                    });
                }
                this.groupedCourses = groupedCourses;
                this.courseCategories = categories;
            },
            createQuiz() {
                if (!this.validateCreateCourse()) {
                    return;
                }
                /**
                 * Quiz Sample Questions:
                 Math Quiz 1
                 1. Are you ready?
                 2. 1 + 1 = ?
                 3. 1 * 1 = ?
                 */

                let selectedCourseItems = this.groupedCourses[this.selectedCourse];
                let that = this;
                let params = new URLSearchParams();
                params.append('eventType', 'Quiz');
                params.append('eventDescription', this.eventDescription);
                params.append('eventValue', this.eventValue);
                params.append('fileReference', "this.fileReference");
                // Todo: Bugs
                selectedCourseItems.forEach(function (item) {
                    let submitParams = params;
                    const receiver = item.receiver.split(', ')[2].split('=')[1];
                    const refLinearId = item.linearId.toString();
                    submitParams.append("partyName", receiver);
                    submitParams.append("refLinearId", refLinearId);
                    // console.log(submitParams.partyName);

                    that.$axios.post(
                        `${that.baseUrl}/create-record`,
                        submitParams
                    ).then(res => {
                        if (res.status === 201) {
                            that.$message({
                                message: 'Create a quiz success!',
                                showClose: true,
                                type: 'success',
                            });
                            // location.reload();
                        }
                        else {
                            that.$message({
                                message: `Create a quiz fail with status ${res.status}...`,
                                showClose: true,
                                type: 'error',
                            });
                        }
                        console.log("Rate conduct status: " + res.status)
                    });
                });

            },
            validateCreateCourse() {
                let isValidTx = true;
                if (this.selectedCourse.length < 1) {
                    this.$message({
                        message: 'Please select one course',
                        showClose: true,
                        type: 'warning',
                    });
                    isValidTx = false;
                }
                if (this.eventValue.length < 1) {
                    this.$message({
                        message: 'Please input quiz name',
                        showClose: true,
                        type: 'warning',
                    });
                    isValidTx = false;
                }
                if (this.eventDescription.length < 1) {
                    this.$message({
                        message: 'Please input quiz questions',
                        showClose: true,
                        type: 'warning',
                    });
                    isValidTx = false;
                }
                return isValidTx;
            },
            handleEventValueChange(val) {
                this.eventValue = val.toString();
                // console.log(this.eventValue);
            },
            handleEventDesChange(val) {
                this.eventDescription = val.toString();
            },
        },
        created() {

        }
    }
</script>

<style scoped>
  .main {
    margin-left: 10%;
    margin-right: 10%;
    text-align: center!important;
    font-size: large!important;
  }
  .create {
    text-align: left!important;
  }
  .clearfix:before,
  .clearfix:after {
    display: table;
    content: "";
  }
  .clearfix:after {
    clear: both
  }
  .text-item {
    text-align: left;
    font-size: medium;
  }

</style>