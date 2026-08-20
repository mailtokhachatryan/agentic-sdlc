import { formatDistanceToNow } from 'date-fns'

export default function TimeAgo({ date }: { date: string }) {
  return <span title={date}>{formatDistanceToNow(new Date(date), { addSuffix: true })}</span>
}
