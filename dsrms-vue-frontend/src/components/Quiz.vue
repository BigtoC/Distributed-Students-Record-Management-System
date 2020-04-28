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
        <el-collapse-item :key="item.ref.txhash" v-for="(item,index) in quiz"
                          :title="item.state.data.eventValue" :name="index"
        >
          <el-card class="box-card" shadow="hover">
            <div slot="header" class="clearfix" >
              <span style="float: left;">Course details</span>
              <div class="teacher-btn-gp" v-if="role === 'Teacher'">
                <el-button style="float: right; padding: 1px 0" type="text">Modify</el-button>
              </div>
              <div class="teacher-btn-gp" v-if="role === 'Student'">
                <el-button style="float: right; padding: 1px 0" type="text"
                           @click="currentQuiz = item.state.data">
                  Answer
                </el-button>
              </div>

            </div>
            <div class="text-item">
              <p><b>Teacher:</b> {{ item.state.data.issuer.split(', ')[0].split('=')[1] }}</p>
              <p><b>Student:</b> {{ item.state.data.receiver.split(', ')[0].split('=')[1] }}</p>
              <b>Quiz Questions:</b><br/>
              <div v-bind:key="question" v-for="question in beautifyQuizQuestions(item.state.data.eventDescriptions)">
                {{ question }}<br/>
              </div>
            </div>
          </el-card>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <br/>
    <div class="answer">
      <el-divider content-position="left">Answer</el-divider>
      <div id="answer-input">
        <el-card class="box-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span>{{ currentQuiz.eventValue }}</span>
            <el-popover style="float: right; padding: 1px 0" placement="top" width="280">
              <div>Are you sure you are ready to submit?</div><br/>
              <div style="text-align: right; margin: 0">
                <el-button type="primary" size="mini" @click="submitAnswer()">
                  Yes! OK!
                </el-button>
              </div>
              <el-button slot="reference" size="small" round>Submit</el-button>
            </el-popover>
          </div>
          <div class="answer-input">
            <el-input type="textarea" :autosize="{ minRows: 2}" :placeholder="currentQuiz.eventValue" v-model="answers">
            </el-input>
          </div>
        </el-card>
      </div>
    </div>

    <div class="quiz-result">
      <br/>
      <el-divider content-position="left">Quiz Results</el-divider>
      <div :key="result.ref.txhash" v-for="result in doQuiz">
        <el-card class="box-card card-header" shadow="hover">
          <div slot="header" class="clearfix">
            <p>{{ result.state.data.eventDescriptions }}</p>
            Grade:
            <span :key="grade.ref.txhash" v-for="grade in grades">
              <span v-if="isGraded(grade, result) === true">
                {{ grade.state.data.eventValue }}
              </span>

            </span>
            <div class="grade-btn-gp" v-if="role === 'Teacher'">
              <el-popover style="float: right; padding: 1px 0" placement="top" width="280">
                <el-input placeholder="Grade this quiz" v-model="teacherGrade" clearable></el-input><br/><br/>
                <div style="text-align: right; margin: 0">
                  <el-button type="primary" size="mini" @click="teacherGrading(result.state.data)">
                    Submit
                  </el-button>
                </div>
                <el-button slot="reference" size="small" round>Grade</el-button>
              </el-popover>
            </div>

          </div>
          <div class="text-item">
            <p><b>Student:</b> {{ result.state.data.issuer.split(', ')[0].split('=')[1] }}</p>
            <b>Answers:</b><br/>
            <div v-bind:key="answer" v-for="answer in beautifyQuizQuestions(result.state.data.eventValue)">
              {{ answer }}<br/>
            </div>
          </div>
        </el-card>
        <br/>
      </div>
    </div>

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
            doQuiz: Array,
            grades: Array,
        },
        data() {
            return {
                activeNames: [],
                groupedCourses: {},
                courseCategories: [],
                eventDescription: '',
                eventValue: '',
                selectedCourse: '',
                answers: '',
                currentQuiz: {},
                teacherGrade: '',
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
            beautifyQuizQuestions(stringLine) {
                console.log(stringLine);
                return stringLine.split('\n')
            },
            createQuiz() {
                if (!this.validateCreateQuiz()) {
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
                    const refLinearId = item.linearId.id.toString();
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
                            location.reload();
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
            submitAnswer() {
                let currentQuiz = this.currentQuiz;
                let eventVal = this.answers;
                let eventDes = currentQuiz.eventValue;
                let partyName = currentQuiz.issuer.split(', ')[2].split('=')[1];
                let refLinearId = currentQuiz.linearId.id.toString();
                let params = new URLSearchParams();
                params.append('eventType', 'DoQuiz');
                params.append('eventDescription', eventDes);
                params.append('eventValue', eventVal);
                params.append('fileReference', "this.fileReference");
                params.append('partyName', partyName);
                params.append("refLinearId", refLinearId);

                this.$axios.post(
                    `${this.baseUrl}/create-record`,
                    params
                ).then(res => {
                    if (res.status === 201) {
                        this.$message({
                            message: 'Submit quiz answer success!',
                            showClose: true,
                            type: 'success',
                        });
                        location.reload();
                    }
                    else {
                        this.$message({
                            message: `Submit quiz answer fail with status ${res.status}...`,
                            showClose: true,
                            type: 'error',
                        });
                    }
                });

            },
            teacherGrading(answerItem) {
                let currentQuiz = answerItem;
                let eventVal = this.teacherGrade;
                let eventDes = currentQuiz.eventDescriptions;
                let partyName = currentQuiz.issuer.split(', ')[2].split('=')[1];
                let refLinearId = currentQuiz.linearId.id.toString();
                let params = new URLSearchParams();
                params.append('eventType', 'Grade');
                params.append('eventDescription', eventDes);
                params.append('eventValue', eventVal);
                params.append('fileReference', "this.fileReference");
                params.append('partyName', partyName);
                params.append("refLinearId", refLinearId);

                this.$axios.post(
                    `${this.baseUrl}/create-record`,
                    params
                ).then(res => {
                    if (res.status === 201) {
                        this.$message({
                            message: 'Grade the quiz answer success!',
                            showClose: true,
                            type: 'success',
                        });
                        location.reload();
                    }
                    else {
                        this.$message({
                            message: `Grade the quiz answer fail with status ${res.status}...`,
                            showClose: true,
                            type: 'error',
                        });
                    }
                });
            },
            isGraded(grade, result) {
                return grade.state.data.refStateId === result.state.data.linearId.id
            },
            validateCreateQuiz() {
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
  .text-item, .card-header {
    text-align: left;
    font-size: medium;
  }

</style>