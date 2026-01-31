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

export default function CodeBlock({ code }: { code: string }) {
  return (
    <div className="bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-sm text-slate-300 overflow-x-auto">
      <pre>{code}</pre>
    </div>
  )
}
