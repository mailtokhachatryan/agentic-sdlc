import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import PlansPage from './pages/PlansPage'
import PlanDetail from './pages/PlanDetail'
import CodingRunsPage from './pages/CodingRunsPage'
import CodingRunDetail from './pages/CodingRunDetail'
import PullRequestsPage from './pages/PullRequestsPage'
import PullRequestDetail from './pages/PullRequestDetail'
import NewPlan from './pages/NewPlan'
import NewCodingRun from './pages/NewCodingRun'
import NewPullRequest from './pages/NewPullRequest'

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/plans" element={<PlansPage />} />
        <Route path="/plans/new" element={<NewPlan />} />
        <Route path="/plans/:id" element={<PlanDetail />} />
        <Route path="/coding-runs" element={<CodingRunsPage />} />
        <Route path="/coding-runs/new" element={<NewCodingRun />} />
        <Route path="/coding-runs/:id" element={<CodingRunDetail />} />
        <Route path="/pull-requests" element={<PullRequestsPage />} />
        <Route path="/pull-requests/new" element={<NewPullRequest />} />
        <Route path="/pull-requests/:id" element={<PullRequestDetail />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}
