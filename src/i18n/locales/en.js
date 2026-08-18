// English locale
export default {
  meta: {
    title: 'EVAN Official Schedule',
    description:
      'EVAN Official Schedule — archive of official schedules: releases, music shows, live streams, offline events, brand campaigns, birthdays and more.'
  },
  nav: {
    label: 'Main navigation',
    schedule: 'Schedule',
    timeline: 'Timeline',
    archive: 'Archive',
    about: 'About'
  },
  bottomNav: {
    label: 'Bottom navigation'
  },
  common: {
    all: 'All',
    language: 'Language'
  },
  hero: {
    officialArchive: 'Official Archive — {year}',
    currentEra: 'CURRENT ERA',
    nextEvent: 'Next Event',
    clickForDetails: 'Click for details'
  },
  subscribe: {
    button: 'Subscribe Calendar',
    eyebrow: 'CALENDAR SUBSCRIPTION',
    title: 'Subscribe on your phone',
    desc: 'Sync {artist}\u2019s schedule to your phone calendar. Updates refresh automatically, and your phone reminds you in time.',
    copy: 'Copy link',
    copied: 'Copied',
    copyFail: 'Failed — long-press the link',
    download: 'Download .ics',
    stepIos: 'iPhone: copy the link → Settings → Calendar → Subscribed Calendars',
    stepAndroid: 'Android: copy the link → open your calendar app → add subscription (entry varies by device)',
    localHint: 'Local preview mode: the link only works on this device until the site is deployed.',
    deployHint: 'Share the link with fans — subscribe once, synced forever.',
    close: 'Close'
  },
  calendar: {
    prevMonth: 'Previous month',
    nextMonth: 'Next month',
    switchView: 'Switch view',
    today: 'Today',
    month: 'Month',
    list: 'List',
    more: '+{n} more',
    empty: 'No events for the selected types.'
  },
  upcoming: {
    title: 'UPCOMING',
    note: 'NEXT 7 EVENTS',
    next: 'NEXT',
    empty: 'No upcoming events for the selected types.'
  },
  timeline: {
    eyebrow: 'Schedule Archive',
    sub: 'A vertical archive of the entire era. {artist}\u2019s official schedule, archived by month — {era} era.',
    eventCount: '{n} EVENT | {n} EVENTS',
    empty: 'No events for the selected types.',
    browseArchive: 'Browse the full Archive →'
  },
  archive: {
    eyebrow: 'Official Archive — {year}',
    sub: 'Archive of past official events. Every entry keeps its original official source record — never fabricated, never rewritten.',
    empty: 'No past events for the selected types.'
  },
  about: {
    eyebrow: 'About This Archive',
    sourcePolicy: 'SOURCE POLICY',
    note: 'This archive site does not create information — it only archives it. Every event is labeled with its official source and credibility level. Unofficial content is never disguised as official.',
    levels: {
      official: {
        name: 'Official channels',
        desc: 'From the artist\u2019s official channels such as Weverse / official SNS / official YouTube. Highest credibility, official announcements take precedence.'
      },
      brand: {
        name: 'Brand channels',
        desc: 'From the official channels of partner brands. The activity itself is an official collaboration, but information follows the brand\u2019s announcements.'
      },
      media: {
        name: 'Media channels',
        desc: 'From TV programs, magazines and media platforms. Content involves official participation, but the publishing channel is a third party.'
      },
      fan: {
        name: 'Fan projects',
        desc: 'Anything not officially confirmed is labeled RUMORED, for reference only and not treated as an official schedule.'
      }
    },
    footer:
      'This page is a fan-made archive site for official schedules and has no affiliation with {artist} or their agency. All schedules are subject to official announcements.',
    footNote: 'EVAN OFFICIAL SCHEDULE — ARCHIVE {year}'
  },
  drawer: {
    label: 'Event details',
    close: 'Close',
    date: 'DATE',
    time: 'TIME',
    location: 'LOCATION',
    countdown: 'COUNTDOWN',
    officialSource: 'Official Source',
    published: 'Published {date}',
    viewOriginal: 'View Original →',
    viewOfficialSource: 'View official source',
    viewSource: 'View source',
    watchLive: 'Watch live',
    joinOnline: 'Join online',
    viewLocation: 'View location',
    navigate: 'Navigate',
    addToCalendar: 'Add to my calendar'
  },
  types: {
    RELEASE: 'RELEASE',
    EVENT: 'EVENT',
    TV: 'TV',
    LIVE: 'LIVE',
    PHOTO: 'PHOTO',
    MAGAZINE: 'MAGAZINE',
    OFFLINE: 'OFFLINE',
    BRAND: 'BRAND',
    BIRTHDAY: 'BIRTHDAY'
  },
  status: {
    CONFIRMED: 'CONFIRMED',
    TBA: 'TBA',
    RUMORED: 'RUMORED'
  },
  sourceLevels: {
    OFFICIAL: 'Verified Source',
    BRAND: 'Brand Source',
    MEDIA: 'Media Source',
    FAN: 'Fan Project'
  }
}
