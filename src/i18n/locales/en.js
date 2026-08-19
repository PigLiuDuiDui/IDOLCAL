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
    comeback: 'Comeback',
    reminders: 'Reminders',
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
  home: {
    switchView: 'Switch view',
    today: 'Today',
    thisWeek: 'This Week',
    calendar: 'Calendar',
    todayLabel: 'TODAY',
    thisWeekLabel: 'THIS WEEK',
    todayTag: 'TODAY',
    todayEmpty: 'No events today. Enjoy the calm \u2014 or check what\u2019s coming up this week.',
    weekEmpty: 'Nothing scheduled in the next 7 days.',
    allDay: 'ALL DAY'
  },
  reminder: {
    setBtn: 'Remind me',
    setBtnDone: 'Reminder set',
    title: 'Remind me',
    eyebrow: 'REMINDER',
    at: 'at',
    confirm: 'Set reminder',
    update: 'Update',
    cancel: 'Cancel reminder',
    setAt: 'Reminder set for {at}',
    noTime: 'This event has no confirmed start time yet.',
    options: {
      '1d': '1 day before',
      '3h': '3 hours before',
      '1h': '1 hour before',
      '30m': '30 minutes before',
      start: 'At start time'
    }
  },
  reminders: {
    eyebrow: 'My Reminders',
    sub: 'All your event reminders in one place. They are stored locally in your browser \u2014 system-level notifications arrive with the upcoming PWA update.',
    alertAt: 'Alert',
    cancel: 'Remove',
    emptyTitle: 'No reminders yet',
    emptyDesc: 'Open any event and tap \u201cRemind me\u201d to get an alert before it starts.',
    browse: 'Browse schedule',
    note: 'Reminders are calculated from the official event timezone and shown in your local time.'
  },
  timezone: {
    select: 'Select timezone',
    eyebrow: 'TIME ZONE',
    auto: 'Auto',
    autoHint: 'Follow device timezone',
    official: 'Official Time',
    yourTime: 'Your time',
    close: 'Close'
  },
  comeback: {
    eyebrow: 'COMEBACK',
    releaseDate: 'RELEASE DATE',
    timeline: 'COMEBACK TIMELINE',
    timelineNote: 'FULL SCHEDULE',
    statuses: {
      COMPLETED: 'COMPLETED',
      UPCOMING: 'UPCOMING',
      TODAY: 'TODAY'
    },
    stages: {
      'concept-photo': 'Concept Photo',
      tracklist: 'Tracklist',
      'highlight-medley': 'Highlight Medley',
      'mv-teaser': 'MV Teaser',
      'album-release': 'Album Release',
      showcase: 'Showcase',
      'music-shows': 'Music Shows'
    }
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
