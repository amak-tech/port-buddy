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
  path: '/docs/guides/minecraft-server',
  title: 'Hosting a Minecraft Server',
  intro: 'Learn how to expose your local Minecraft server to the internet using Port Buddy, allowing your '
    + 'friends to join without port forwarding or configuring your router.',
  prerequisites: [
    'A running Minecraft Server (Java or Bedrock Edition) on your local machine.',
    'Port Buddy CLI installed and authenticated.'
  ],
  sections: [
    {
      name: 'Java Edition',
      intro: 'Minecraft Java Edition uses TCP port **25565** by default.',
      steps: [
        {
          name: 'Start your Minecraft Server',
          text: 'Ensure your server is running and accessible locally (usually at `localhost:25565`).'
        },
        {
          name: 'Expose the port',
          text: 'Run the following command in your terminal:',
          command: 'portbuddy tcp 25565',
          resultText: 'You will see output similar to this:',
          resultOutput: 'tcp localhost:25565 exposed to: net-proxy-1.portbuddy.dev:42123'
        },
        {
          name: 'Connect',
          text: 'Share the address (e.g., `net-proxy-1.portbuddy.dev:42123`) with your friends. They can '
            + 'enter this address in the Multiplayer menu under "Direct Connection" or "Add Server".'
        }
      ]
    },
    {
      name: 'Bedrock Edition',
      intro: 'Minecraft Bedrock Edition uses UDP port **19132** by default.',
      steps: [
        {
          name: 'Start your Bedrock Server',
          text: 'Ensure your server is running locally.'
        },
        {
          name: 'Expose the port',
          text: 'Run the following command:',
          command: 'portbuddy udp 19132'
        },
        {
          name: 'Connect',
          text: 'Share the generated address and port with your friends. They can add it to their server list.'
        }
      ]
    }
  ]
}

export default function MinecraftGuide() {
  return <GuideArticle guide={guide} />
}
