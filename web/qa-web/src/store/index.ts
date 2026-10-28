import { reactive } from 'vue';
import type { Locale } from '../locales';
import { fetchDoctors, loginDoctor as loginDoctorApi } from '../api/doctors';
import {
  loginPatient as loginPatientApi,
  registerPatient as registerPatientApi
} from '../api/patients';
import type { PatientRegisterPayload } from '../api/patients';
import { ApiError } from '../api/http';
import questionData from '../data/question-list.json';

export interface Doctor {
  id: string;
  username: string;
  name: string;
  nameEn: string;
  title: string;
  titleEn: string;
  department: string;
  departmentEn: string;
  avatar: string;
  experience: string;
  experienceEn: string;
  specialties: string[];
  specialtiesEn: string[];
  isActive: boolean;
}

export interface Patient {
  id: string;
  username: string;
  name: string;
  birthday: string;
  phone: string;
  gender: string;
}

export interface Question {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  question: string;
  submitTime: string;
  status: 'pending' | 'answered';
  answer: string | null;
  answerTime: string | null;
}

interface State {
  doctors: Doctor[];
  doctorsLoading: boolean;
  doctorsError: string | null;
  questions: Question[];
  currentDoctor: Doctor | null;
  currentPatient: Patient | null;
  locale: Locale;
}

const state = reactive<State>({
  doctors: [],
  doctorsLoading: false,
  doctorsError: null,
  questions: questionData as Question[],
  currentDoctor: null,
  currentPatient: null,
  locale: 'zh-CN',
});

export const store = {
  state,

  setLocale(locale: Locale): void {
    state.locale = locale;
  },

  async loadDoctors(): Promise<void> {
    state.doctorsLoading = true;
    state.doctorsError = null;

    try {
      const page = await fetchDoctors({ page: 1, size: 100 });
      state.doctors = page.items;
    } catch (error) {
      state.doctorsError = error instanceof Error ? error.message : '医生数据加载失败';
      throw error;
    } finally {
      state.doctorsLoading = false;
    }
  },

  async loginDoctor(username: string, password: string): Promise<Doctor | null> {
    try {
      const { doctor } = await loginDoctorApi(username, password);
      state.currentDoctor = doctor;
      return doctor;
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        return null;
      }
      throw error;
    }
  },

  logoutDoctor() {
    state.currentDoctor = null;
  },

  async loginPatient(username: string, password: string): Promise<Patient> {
    const { patient } = await loginPatientApi(username, password);
    state.currentPatient = patient;
    return patient;
  },

  async registerPatient(payload: PatientRegisterPayload): Promise<Patient> {
    const { patient } = await registerPatientApi(payload);
    state.currentPatient = patient;
    return patient;
  },

  logoutPatient() {
    state.currentPatient = null;
  },

  getQuestionsByDoctor(doctorId: string): Question[] {
    return state.questions.filter(q => q.doctorId === doctorId);
  },

  getQuestionsByPatient(patientId: string): Question[] {
    return state.questions.filter(q => q.patientId === patientId);
  },

  addQuestion(question: Omit<Question, 'id' | 'submitTime' | 'status' | 'answer' | 'answerTime'>): Question {
    const newQuestion: Question = {
      ...question,
      id: `q${Date.now()}`,
      submitTime: new Date().toISOString(),
      status: 'pending',
      answer: null,
      answerTime: null,
    };
    state.questions.push(newQuestion);
    return newQuestion;
  },

  answerQuestion(questionId: string, answer: string) {
    const question = state.questions.find(q => q.id === questionId);
    if (question) {
      question.status = 'answered';
      question.answer = answer;
      question.answerTime = new Date().toISOString();
    }
  },

  markQuestionAsAnswered(questionId: string) {
    const question = state.questions.find(q => q.id === questionId);
    if (question) {
      question.status = 'answered';
      question.answer = '已口述解答';
      question.answerTime = new Date().toISOString();
    }
  },

  getDoctorByUsername(username: string): Doctor | undefined {
    return state.doctors.find(d => d.username === username);
  },

  getActiveDoctors(): Doctor[] {
    return state.doctors.filter(d => d.isActive);
  },

  getStatistics() {
    const totalDoctors = state.doctors.length;
    const totalQuestions = state.questions.length;
    const activeSessions = state.questions.filter(q => q.status === 'pending').length;
    const totalSessions = state.doctors.filter(d => d.isActive).length;

    return {
      totalDoctors,
      totalQuestions,
      activeSessions,
      totalSessions,
    };
  },
};
