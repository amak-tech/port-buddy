/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import GuideArticle, { type GuideDef } from './GuideArticle'

const guide: GuideDef = {
  path: '/docs/guides/hytale-server',
  title: 'Hosting a Hytale Server',
  intro: 'Learn how to expose your local Hytale server to the internet using Port Buddy, allowing your '
    + 'friends to join your adventure without complex network configuration.',
  prerequisites: [
    'A running Hytale Server on your local machine.',
    'Port Buddy CLI installed and authenticated.'
  ],
  sections: [
    {
      name: 'Exposing the Server',
      intro: 'Hytale servers use UDP port **5520** by default.',
      steps: [
        {
          name: 'Start your Hytale Server',
          text: 'Launch your Hytale server and ensure it is running locally.'
        },
        {
          name: 'Expose the port',
          text: 'Run the following command in your terminal:',
          command: 'portbuddy udp 5520',
          resultText: 'You will see output similar to this:',
          resultOutput: 'udp localhost:5520 exposed to: net-proxy-2.portbuddy.dev:54321'
        },
        {
          name: 'Connect',
          text: 'Share the address (e.g., `net-proxy-2.portbuddy.dev:54321`) with your friends. They can '
            + 'use this address to connect to your server from the game client.'
        }
      ]
    }
  ]
}

export default function HytaleGuide() {
  return <GuideArticle guide={guide} />
}
